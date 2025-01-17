package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MatchRequest;
import org.borg.backend.game.multiplayer.dto.MatchResponse;
import org.borg.backend.game.multiplayer.dto.RematchRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.PendingSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.borg.backend.shared.enums.BusinessErrorCodes.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerMatchService {

    private final NotificationService notificationService;
    private final PendingSessionRepository pendingSessionRepository;
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final PlayerService playerService;
    private final PlayerBlockService playerBlockService;

    @Transactional
    public void requestMatch(MatchRequest matchRequest) {
        log.info("Processing match request from player {} to player {}",
                matchRequest.getSenderId(), matchRequest.getReceiverId());

        Player sendingPlayer = playerService.getPlayerById(matchRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(matchRequest.getReceiverId());

        handleMatchRequest(sendingPlayer, receivingPlayer, null);
        log.debug("Match request processed successfully between players {} and {}",
                sendingPlayer.getId(), receivingPlayer.getId());
    }

    @Transactional
    public void requestRematch(RematchRequest rematchRequest) {
        log.info("Processing rematch request from player {} for session {}",
                rematchRequest.getPlayerId(), rematchRequest.getSessionId());

        MultiplayerSession session = multiplayerSessionRepository.findById(rematchRequest.getSessionId())
                .orElseThrow(() -> {
                    log.error("Multiplayer session {} not found", rematchRequest.getSessionId());
                    return new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                            String.format("Multiplayer session with ID %d not found", rematchRequest.getSessionId()));
                });
        
        if (session.getStatus().equals(GameStatus.ACTIVE)) {
            log.warn("Attempted rematch request for active session {}", session.getId());
            throw new GameException(GAME_ALREADY_ONGOING,
                    String.format("Cannot request rematch - game session %d is still active", session.getId()));
        }
        
        Long playerId = rematchRequest.getPlayerId();
        
        SessionPlayer sendingPlayer = session.getSessionPlayerByPlayerId(playerId);
        if (sendingPlayer == null) {
            log.error("Player {} not found in session {}", playerId, session.getId());
            throw new GameException(INVALID_SESSION_STATE,
                    String.format("Player %d is not part of session %d", playerId, session.getId()));
        }
        
        SessionPlayer opponentPlayer = session.getOpponentSessionPlayerId(playerId);
        if (opponentPlayer == null) {
            log.error("Opponent not found in session {}", session.getId());
            throw new GameException(INVALID_SESSION_STATE,
                    String.format("Could not find opponent in session %d", session.getId()));
        }
        
        handleMatchRequest(sendingPlayer.getPlayer(), opponentPlayer.getPlayer(), session);
        log.debug("Rematch request processed successfully for old session {}", session.getId());
    }

    private void handleMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        log.debug("Handling match request between players {} and {}",
                sendingPlayer.getId(), receivingPlayer.getId());
        validateMatchRequest(sendingPlayer.getId(), receivingPlayer.getId());

        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(receivingPlayer.getId(), sendingPlayer.getId());

        if (pendingSession != null) {
            log.info("Found simultaneous match request between players {} and {}",
                    sendingPlayer.getId(), receivingPlayer.getId());
            handleSimultaneousRequests(sendingPlayer, receivingPlayer, originalSession, pendingSession);
        } else {
            createNewMatchRequest(sendingPlayer, receivingPlayer, originalSession);
        }
    }

    private void validateMatchRequest(Long senderId, Long receiverId) {
        log.debug("Validating match request between players {} and {}", senderId, receiverId);

        if (multiplayerSessionRepository.checkIfOngoingSessionExists(senderId, receiverId, GameStatus.ACTIVE)) {
            log.warn("Attempted match request while active game exists between players {} and {}",
                    senderId, receiverId);
            throw new GameException(GAME_ALREADY_ONGOING,
                    String.format("Active game already exists between players %d and %d",
                            senderId, receiverId));
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(senderId, receiverId)) {
            log.warn("Duplicate match request from player {} to player {}", senderId, receiverId);
            throw new GameException(REMATCH_REQUEST_ALREADY_SENT,
                    String.format("Player %d has already sent a match request to player %d",
                            senderId, receiverId));
        }

        if (playerBlockService.checkIfBlockIsActive(senderId, receiverId)) {
            log.warn("Player {} attempted to send match request to blocked player {}",
                    senderId, receiverId);
            throw new BlockException(CANNOT_SEND_MATCH_REQUEST_TO_BLOCKED,
                    String.format("Player %d tried to send a match request to blocked player %d",
                            senderId, receiverId));
        }
    }

    private void handleSimultaneousRequests(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession, PendingSession pendingSession) {
        log.info("Processing simultaneous match requests between players {} and {}",
                sendingPlayer.getId(), receivingPlayer.getId());
        
        Player startingPlayer = Math.random() < 0.5 ? sendingPlayer : receivingPlayer;
        log.debug("Randomly selected player {} to start the game", startingPlayer.getId());
        
        Long newSessionId = multiplayerSessionRepository.save(
                new MultiplayerSession(sendingPlayer, receivingPlayer, startingPlayer.getId())).getId();
        log.info("Created new multiplayer session {} for players {} and {}",
                newSessionId, sendingPlayer.getId(), receivingPlayer.getId());

        if (originalSession != null) {
            notificationService.deleteMatchRequestNotification(sendingPlayer.getId(), pendingSession.getId());
        }
        pendingSessionRepository.delete(pendingSession);

        if (originalSession != null) {
            notificationService.sendRematchStartedNotification(sendingPlayer.getId(), receivingPlayer.getDisplayName(), newSessionId);
            notificationService.sendRematchStartedNotification(receivingPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);
            log.info("Sent rematch started notifications for session {}", newSessionId);
        } else {
            notificationService.sendMatchStartedNotification(sendingPlayer.getId(), receivingPlayer.getDisplayName(), newSessionId);
            notificationService.sendMatchStartedNotification(receivingPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);
            log.info("Sent match started notifications for session {}", newSessionId);
        }
    }

    private void createNewMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        Long senderId = sendingPlayer.getId();
        Long receiverId = receivingPlayer.getId();
        
        log.info("Creating new match request from player {} to player {}",
                senderId, receiverId);
        
        PendingSession newSession = pendingSessionRepository.save(new PendingSession(senderId, receiverId));
        log.debug("Created pending session {} for match request", newSession.getId());

        boolean isHidden = playerBlockService.checkIfBlockIsActive(receiverId, senderId);

        if (originalSession != null) {
            notificationService.sendRematchRequestNotification(receiverId, senderId,
                    sendingPlayer.getDisplayName(), newSession.getId(), isHidden);
            log.info("Sent rematch request notification for session {}", newSession.getId());
        } else {
            notificationService.sendMatchRequestNotification(receiverId, senderId,
                    sendingPlayer.getDisplayName(), newSession.getId(), isHidden);
            log.info("Sent match request notification for session {}", newSession.getId());
        }
    }

    @Transactional
    public Long handleMatchAccept(MatchResponse response) {
        log.info("Processing match acceptance for pending session {}", response.getPendingSessionId());

        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> {
                    log.error("Pending session {} not found", response.getPendingSessionId());
                    return new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                            String.format("Pending session with ID %d not found",
                                    response.getPendingSessionId()));
                });

        if (!pendingSession.getOpponentId().equals(response.getSenderId())) {
            log.error("Unauthorized match acceptance attempt by player {} for session {}",
                    response.getSenderId(), response.getPendingSessionId());
            throw new AccessDeniedException(
                    String.format("Player %d not authorized to accept match request %d",
                            response.getSenderId(), response.getPendingSessionId()));
        }

        Long newSessionId = createMultiplayerSession(pendingSession);
        log.info("Created new multiplayer session {} from pending session {}",
                newSessionId, pendingSession.getId());

        if (response.isRematch()) {
            notificationService.sendRematchAcceptedNotification(response.getReceiverId(),
                    response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
            log.debug("Sent rematch accepted notification for session {}", newSessionId);
        } else {
            notificationService.sendMatchAcceptedNotification(response.getReceiverId(),
                    response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
            log.debug("Sent match accepted notification for session {}", newSessionId);
        }

        pendingSessionRepository.delete(pendingSession);
        return newSessionId;
    }

    private Long createMultiplayerSession(PendingSession pendingSession) {
        log.debug("Creating multiplayer session from pending session {}", pendingSession.getId());

        Player player1 = playerService.getPlayerById(pendingSession.getRequestingPlayerId());
        Player player2 = playerService.getPlayerById(pendingSession.getOpponentId());

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;
        log.debug("Randomly selected player {} to start the game", startingPlayer.getId());

        MultiplayerSession session = multiplayerSessionRepository.save(
                new MultiplayerSession(player1, player2, startingPlayer.getId()));
        log.info("Created new multiplayer session {} for players {} and {}",
                session.getId(), player1.getId(), player2.getId());

        return session.getId();
    }

    @Transactional
    public void handleMatchReject(MatchResponse response) {
        log.info("Processing match rejection for pending session {}", response.getPendingSessionId());

        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> {
                    log.error("Pending session {} not found", response.getPendingSessionId());
                    return new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                            String.format("Pending session with ID %d not found",
                                    response.getPendingSessionId()));
                });

        if (response.isRematch()) {
            notificationService.sendRematchRejectedNotification(response.getReceiverId(),
                    response.getPlayerDisplayName(), response.getNotificationId());
            log.debug("Sent rematch rejected notification for session {}",
                    response.getPendingSessionId());
        } else {
            notificationService.sendMatchRejectedNotification(response.getReceiverId(),
                    response.getPlayerDisplayName(), response.getNotificationId());
            log.debug("Sent match rejected notification for session {}",
                    response.getPendingSessionId());
        }

        pendingSessionRepository.delete(pendingSession);
    }
}
