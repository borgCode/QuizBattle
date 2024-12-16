package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchMakingService {

    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final PendingSessionRepository pendingSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRepository playerRepository;


    public void findMatch(Long playerId) {
        synchronized (matchmakingQueue) {
            log.warn("Finding first player in queue");
            Optional<Long> opponentId = matchmakingQueue.stream().findFirst();

            if (opponentId.isPresent()) {
                log.warn("Found opponent in queue");
                matchmakingQueue.remove(opponentId.get());

                handleMatchMakingRequest(playerId, opponentId);

            } else {
                log.warn("Sending waiting to players");
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


        PendingSession pendingSession = new PendingSession(playerId, opponentId.get());
        pendingSessionRepository.save(pendingSession);

        messagingTemplate.convertAndSend("/topic/match" + requestingPlayer.getId(),
                MatchmakingResponse.matched(pendingSession.getId(), opponent.getDisplayName()));
        messagingTemplate.convertAndSend("/topic/match" + opponent.getId(),
                MatchmakingResponse.matched(pendingSession.getId(), requestingPlayer.getDisplayName()));
    }

    public void handleMatchResponse(long pendingSessionId, long playerId, boolean hasAccepted) {
        PendingSession pendingSession = pendingSessionRepository.findById(pendingSessionId)
                .orElseThrow(() -> new NoSuchElementException("Pending session not found"));
        
        if (!hasAccepted) {
            cancelMatch(pendingSession);
            return;
        }
        
        if (playerId == pendingSession.getRequestingPlayerId()) {
            pendingSession.setRequestingPlayerAccepted(true);
        } else if (playerId == pendingSession.getOpponentId()) {
            pendingSession.setOpponentAccepted(true);
        }
        pendingSessionRepository.save(pendingSession);
        
        if (pendingSession.isOpponentAccepted() && pendingSession.isRequestingPlayerAccepted()) {
            createMultiplayerSession(pendingSession);
        }
        
        
    }
    
    private void cancelMatch(PendingSession pendingSession) {
        
        messagingTemplate.convertAndSend("/topic/match" + pendingSession.getRequestingPlayerId(),
                MatchmakingResponse.declined());
        messagingTemplate.convertAndSend("/topic/match" + pendingSession.getOpponentId(),
                MatchmakingResponse.declined());
        pendingSessionRepository.delete(pendingSession);
    }

    private void createMultiplayerSession(PendingSession pendingSession) {
        Player player1 = playerRepository.findById(pendingSession.getRequestingPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
        Player player2 = playerRepository.findById(pendingSession.getOpponentId())
                .orElseThrow(() -> new NoSuchElementException("Opponent not found"));

        //Randomly choose who starts

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = new MultiplayerSession(player1, player2, startingPlayer);
        multiplayerSessionRepository.save(session);

        messagingTemplate.convertAndSend("/topic/match" + player1.getId(),
                MatchmakingResponse.accepted(session.getId(), player2.getDisplayName()));
        messagingTemplate.convertAndSend("/topic/match" + player2.getId(),
                MatchmakingResponse.accepted(session.getId(), player1.getDisplayName()));
        
        pendingSessionRepository.delete(pendingSession);
        
    }

    public void cancelMatchmaking(Long playerId) {
        matchmakingQueue.remove(playerId);
    }
}
