package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.game.multiplayer.model.MatchmakingSession;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MatchmakingSessionRepository;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchMakingService {

    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final MatchmakingSessionRepository matchmakingSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRepository playerRepository;


    public void findMatch(Long playerId) {
        synchronized (matchmakingQueue) {
            Optional<Long> opponentId = matchmakingQueue.stream().findFirst();
            if (opponentId.isPresent()) {
                if (opponentId.get().equals(playerId)) {
                    return;
                }
                matchmakingQueue.remove(opponentId.get());
                handleMatchMakingRequest(playerId, opponentId);
            } else {
                matchmakingQueue.add(playerId);
                messagingTemplate.convertAndSend("/topic/match" + playerId,
                        MatchmakingResponse.waiting());
            }
        }
    }

    private void handleMatchMakingRequest(Long playerId, Optional<Long> opponentId) {

        Player requestingPlayer = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
        Player opponent = playerRepository.findById(opponentId.get())
                .orElseThrow(() -> new NoSuchElementException("Opponent not found"));


        MatchmakingSession matchmakingSession = new MatchmakingSession(playerId, opponentId.get());
        matchmakingSessionRepository.save(matchmakingSession);

        messagingTemplate.convertAndSend("/topic/match" + requestingPlayer.getId(),
                MatchmakingResponse.matched(matchmakingSession.getId(), opponent.getDisplayName()));
        messagingTemplate.convertAndSend("/topic/match" + opponent.getId(),
                MatchmakingResponse.matched(matchmakingSession.getId(), requestingPlayer.getDisplayName()));
    }

    public void handleMatchResponse(long matchmakingSessionId, long playerId, boolean hasAccepted) {
        MatchmakingSession matchmakingSession = matchmakingSessionRepository.findById(matchmakingSessionId)
                .orElseThrow(() -> new NoSuchElementException("Matchmaking session not found"));
        
        if (!hasAccepted) {
            try {
                cancelMatch(matchmakingSession, playerId);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("Session {} was already cancelled by another player", matchmakingSession);
            }
            return;
        }
        
        if (playerId == matchmakingSession.getRequestingPlayerId()) {
            matchmakingSession.setRequestingPlayerAccepted(true);
        } else if (playerId == matchmakingSession.getOpponentId()) {
            matchmakingSession.setOpponentAccepted(true);
        }
        matchmakingSessionRepository.save(matchmakingSession);
        
        if (matchmakingSession.isOpponentAccepted() && matchmakingSession.isRequestingPlayerAccepted()) {
            createMultiplayerSession(matchmakingSession);
        } else {
            messagingTemplate.convertAndSend("/topic/match" + playerId,
                    MatchmakingResponse.waitingForOtherPlayer());
        }
    }
    
    private void cancelMatch(MatchmakingSession matchmakingSession, long playerId) {
        Long opponentId = matchmakingSession.getOpponentId().equals(playerId) ? matchmakingSession.getRequestingPlayerId() : matchmakingSession.getOpponentId();
        
        messagingTemplate.convertAndSend("/topic/match" + opponentId,
                MatchmakingResponse.declined());
        matchmakingSessionRepository.delete(matchmakingSession);
    }

    private void createMultiplayerSession(MatchmakingSession matchmakingSession) {
        Player player1 = playerRepository.findById(matchmakingSession.getRequestingPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
        Player player2 = playerRepository.findById(matchmakingSession.getOpponentId())
                .orElseThrow(() -> new NoSuchElementException("Opponent not found"));

        //Randomly choose who starts

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = new MultiplayerSession(player1, player2, startingPlayer);
        multiplayerSessionRepository.save(session);

        messagingTemplate.convertAndSend("/topic/match" + player1.getId(),
                MatchmakingResponse.accepted(session.getId(), player2.getDisplayName()));
        messagingTemplate.convertAndSend("/topic/match" + player2.getId(),
                MatchmakingResponse.accepted(session.getId(), player1.getDisplayName()));
        
        matchmakingSessionRepository.delete(matchmakingSession);
        
    }

    public void cancelMatchmaking(Long playerId) {
        matchmakingQueue.remove(playerId);
    }
    
    //Test methods
    public void clearQueue() {
        this.matchmakingQueue.clear();
    }
    
    public int getQueueSize() {
        synchronized (matchmakingQueue) {
            return matchmakingQueue.size();
        }
    }
}
