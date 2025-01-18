package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.game.multiplayer.model.MatchmakingSession;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MatchmakingSessionRepository;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchMakingService {

    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final MatchmakingSessionRepository matchmakingSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerService playerService;
    private final PlayerBlockService playerBlockService;

    public void findMatch(Long playerId) {
        log.debug("Player {} looking for match", playerId);
        synchronized (matchmakingQueue) {
            Optional<Long> opponentId = matchmakingQueue.stream().findFirst();
            if (opponentId.isPresent()) {
                Long actualOpponentId = opponentId.get();
                if (actualOpponentId.equals(playerId)) {
                    log.debug("Player {} attempted to match with self, ignoring", playerId);
                    return;
                }
                if (playerBlockService.checkIfAnyBlockExists(playerId, actualOpponentId)) {
                    log.debug("Player {} attempted to match with {} while block is in place, ignoring", playerId, actualOpponentId);

                    matchmakingQueue.add(playerId);
                    log.debug("Player {} added to matchmaking queue. Queue size: {}", playerId, matchmakingQueue.size());
                    messagingTemplate.convertAndSend("/topic/match" + playerId,
                            MatchmakingResponse.waiting());
                    return;
                }
                
                matchmakingQueue.remove(actualOpponentId);
                log.info("Match found: Player {} matched with Player {}", playerId, actualOpponentId);
                handleMatchMakingRequest(playerId, actualOpponentId);
            } else {
                matchmakingQueue.add(playerId);
                log.debug("Player {} added to matchmaking queue. Queue size: {}", playerId, matchmakingQueue.size());
                messagingTemplate.convertAndSend("/topic/match" + playerId,
                        MatchmakingResponse.waiting());
            }
        }
    }

    private void handleMatchMakingRequest(Long playerId, Long opponentId) {
        log.debug("Creating matchmaking session for players {} and {}", playerId, opponentId);
        Player requestingPlayer = playerService.getPlayerById(playerId);
        Player opponent = playerService.getPlayerById(opponentId);

        MatchmakingSession matchmakingSession = new MatchmakingSession(playerId, opponentId);
        matchmakingSessionRepository.save(matchmakingSession);
        log.info("Created matchmaking session {} for players {} and {}",
                matchmakingSession.getId(), requestingPlayer.getDisplayName(), opponent.getDisplayName());


        messagingTemplate.convertAndSend("/topic/match" + requestingPlayer.getId(),
                MatchmakingResponse.matched(matchmakingSession.getId(), opponent.getDisplayName()));
        messagingTemplate.convertAndSend("/topic/match" + opponent.getId(),
                MatchmakingResponse.matched(matchmakingSession.getId(), requestingPlayer.getDisplayName()));
    }

    public void handleMatchResponse(long matchmakingSessionId, long playerId, boolean hasAccepted) {
        log.debug("Received match response from player {}: {} for session {}",
                playerId, hasAccepted ? "accepted" : "declined", matchmakingSessionId);

        MatchmakingSession matchmakingSession = matchmakingSessionRepository.findById(matchmakingSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Matchmaking session not found for " + matchmakingSessionId));

        if (matchmakingSession == null) {
            log.debug("Ignoring response for non-existent session {}", matchmakingSessionId);
            return;
        }
        
        if (!hasAccepted) {
            try {
                log.info("Player {} declined match in session {}", playerId, matchmakingSessionId);
                cancelMatch(matchmakingSession, playerId);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("Session {} was already cancelled by another player", matchmakingSession);
            }
            return;
        }

        if (playerId == matchmakingSession.getPlayer1Id()) {
            matchmakingSession.setPlayer1Accepted(true);
            log.debug("Requesting player {} accepted match", playerId);
        } else if (playerId == matchmakingSession.getPlayer2Id()) {
            log.debug("Opponent {} accepted match", playerId);
            matchmakingSession.setPlayer2Accepted(true);
        }
        matchmakingSessionRepository.save(matchmakingSession);

        if (matchmakingSession.isPlayer2Accepted() && matchmakingSession.isPlayer1Accepted()) {
            log.info("Both players accepted match in session {}", matchmakingSessionId);
            createMultiplayerSession(matchmakingSession);
        } else {
            log.debug("Waiting for other player's response in session {}", matchmakingSessionId);
            messagingTemplate.convertAndSend("/topic/match" + playerId,
                    MatchmakingResponse.waitingForOtherPlayer());
        }
    }

    private void cancelMatch(MatchmakingSession matchmakingSession, long playerId) {
        Long opponentId = matchmakingSession.getPlayer2Id().equals(playerId) ? matchmakingSession.getPlayer1Id() : matchmakingSession.getPlayer2Id();

        log.info("Cancelling match session {} between players {} and {}",
                matchmakingSession.getId(), playerId, opponentId);
        messagingTemplate.convertAndSend("/topic/match" + opponentId,
                MatchmakingResponse.declined());
        matchmakingSessionRepository.delete(matchmakingSession);
    }

    private void createMultiplayerSession(MatchmakingSession matchmakingSession) {
        Player player1 = playerService.getPlayerById(matchmakingSession.getPlayer1Id());
        Player player2 = playerService.getPlayerById(matchmakingSession.getPlayer2Id());

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = new MultiplayerSession(player1, player2, startingPlayer.getId());
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
