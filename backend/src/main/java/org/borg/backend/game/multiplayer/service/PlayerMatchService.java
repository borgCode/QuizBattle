package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MatchRequest;
import org.borg.backend.game.multiplayer.dto.MatchResponse;
import org.borg.backend.game.multiplayer.dto.RematchRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.PendingSession;
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
        Player sendingPlayer = playerService.getPlayerById(matchRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(matchRequest.getReceiverId());

        handleMatchRequest(sendingPlayer, receivingPlayer, null);
    }

    @Transactional
    public void requestRematch(RematchRequest rematchRequest) {
        MultiplayerSession session = multiplayerSessionRepository.findById(rematchRequest.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                        String.format("Multiplayer session with ID %d not found", rematchRequest.getSessionId())));

        if (session.getStatus().equals(GameStatus.ACTIVE)) {
            throw new GameException(GAME_ALREADY_ONGOING,
                    String.format("Cannot request rematch - game session %d is still active", session.getId()));
        }

        Long playerId = rematchRequest.getPlayerId();

        Player sendingPlayer = session.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException(INVALID_SESSION_STATE,
                        String.format("Player %d is not part of session %d", playerId, session.getId())));

        Player opponentPlayer = session.getPlayers().stream()
                .filter(player -> !player.equals(sendingPlayer))
                .findFirst()
                .orElseThrow(() -> new GameException(INVALID_SESSION_STATE,
                        String.format("Could not find opponent in session %d", session.getId())));

        handleMatchRequest(sendingPlayer, opponentPlayer, session);
    }

    private void handleMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        validateMatchRequest(sendingPlayer.getId(), receivingPlayer.getId());

        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(receivingPlayer.getId(), sendingPlayer.getId());

        if (pendingSession != null) {
            handleSimultaneousRequests(sendingPlayer, receivingPlayer, originalSession, pendingSession);
        } else {
            createNewMatchRequest(sendingPlayer, receivingPlayer, originalSession);
        }
    }

    private void validateMatchRequest(Long senderId, Long receiverId) {
        if (multiplayerSessionRepository.checkIfOngoingSessionExists(senderId, receiverId, GameStatus.ACTIVE)) {
            throw new GameException(GAME_ALREADY_ONGOING,
                    String.format("Active game already exists between players %d and %d",
                            senderId, receiverId));
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(senderId, receiverId)) {
            throw new GameException(REMATCH_REQUEST_ALREADY_SENT,
                    String.format("Player %d has already sent a match request to player %d",
                            senderId, receiverId));
        }
        if (playerBlockService.checkIfBlockIsActive(senderId, receiverId)) {
            throw new BlockException(CANNOT_SEND_MATCH_REQUEST_TO_BLOCKED,
                    String.format("Player %d tried to send a match request to blocked player %d", senderId, receiverId));
        }
    }

    private void handleSimultaneousRequests(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession, PendingSession pendingSession) {
        Player startingPlayer = Math.random() < 0.5 ? sendingPlayer : receivingPlayer;

        Long newSessionId = multiplayerSessionRepository.save(
                new MultiplayerSession(sendingPlayer, receivingPlayer, startingPlayer)).getId();

        if (originalSession != null) {
            notificationService.deleteMatchRequestNotification(sendingPlayer.getId(), pendingSession.getId());
        }
        pendingSessionRepository.delete(pendingSession);

        if (originalSession != null) {
            notificationService.sendRematchStartedNotification(sendingPlayer.getId(), receivingPlayer.getDisplayName(), newSessionId);
            notificationService.sendRematchStartedNotification(receivingPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);
        } else {
            notificationService.sendMatchStartedNotification(sendingPlayer.getId(), receivingPlayer.getDisplayName(), newSessionId);
            notificationService.sendMatchStartedNotification(receivingPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);
        }
    }

    private void createNewMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        PendingSession newSession = pendingSessionRepository.save(new PendingSession(sendingPlayer.getId(), receivingPlayer.getId()));

        if (originalSession != null) {
            notificationService.sendRematchRequestNotification(receivingPlayer.getId(), sendingPlayer.getId(), sendingPlayer.getDisplayName(), newSession.getId());
        } else {
            notificationService.sendMatchRequestNotification(receivingPlayer.getId(), sendingPlayer.getId(), sendingPlayer.getDisplayName(), newSession.getId());
        }
    }

    @Transactional
    public Long handleMatchAccept(MatchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                        String.format("Pending session with ID %d not found", response.getPendingSessionId())));

        if (!pendingSession.getOpponentId().equals(response.getSenderId())) {
            throw new AccessDeniedException(
                    String.format("Player %d not authorized to accept match request %d",
                            response.getSenderId(), response.getPendingSessionId()));
        }

        Long newSessionId = createMultiplayerSession(pendingSession);

        if (response.isRematch()) {
            notificationService.sendRematchAcceptedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
        } else {
            notificationService.sendMatchAcceptedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
        }
        pendingSessionRepository.delete(pendingSession);
        return newSessionId;
    }

    private Long createMultiplayerSession(PendingSession pendingSession) {
        Player player1 = playerService.getPlayerById(pendingSession.getRequestingPlayerId());
        Player player2 = playerService.getPlayerById(pendingSession.getOpponentId());

        //Randomly choose who starts

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = multiplayerSessionRepository.save(
                new MultiplayerSession(player1, player2, startingPlayer));
        return session.getId();
    }

    @Transactional
    public void handleMatchReject(MatchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND,
                        String.format("Pending session with ID %d not found", response.getPendingSessionId())));

        if (response.isRematch()) {
            notificationService.sendRematchRejectedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId());
        } else {
            notificationService.sendMatchRejectedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId());
        }
        pendingSessionRepository.delete(pendingSession);
    }
}
