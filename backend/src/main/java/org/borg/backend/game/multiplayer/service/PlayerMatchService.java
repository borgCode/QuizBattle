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
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
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
    private final PlayerRepository playerRepository;
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final PlayerService playerService;

    @Transactional
    public void requestMatch(MatchRequest matchRequest) {
        Player sendingPlayer = playerService.getPlayerById(matchRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(matchRequest.getReceiverId());

        handleMatchRequest(sendingPlayer, receivingPlayer, null);
    }

    @Transactional
    public void requestRematch(RematchRequest rematchRequest) {
        MultiplayerSession session = multiplayerSessionRepository.findById(rematchRequest.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND, "Session not found for " + rematchRequest.getSessionId()));

        if (session.getStatus().equals(GameStatus.ACTIVE)) {
            throw new GameException(GAME_ALREADY_ONGOING);
        }

        Long playerId = rematchRequest.getPlayerId();

        Player sendingPlayer = session.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException(INVALID_SESSION_STATE));

        Player opponentPlayer = session.getPlayers().stream()
                .filter(player -> !player.equals(sendingPlayer))
                .findFirst()
                .orElseThrow(() -> new GameException(INVALID_SESSION_STATE));

        handleMatchRequest(sendingPlayer, opponentPlayer, session);
    }

    private void handleMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        if (multiplayerSessionRepository.checkIfOngoingSessionExists(sendingPlayer, receivingPlayer, GameStatus.ACTIVE)) {
            throw new GameException(GAME_ALREADY_ONGOING);
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), receivingPlayer.getId())) {
            throw new GameException(REMATCH_REQUEST_ALREADY_SENT);
        }
        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(receivingPlayer.getId(), sendingPlayer.getId());

        if (pendingSession != null) {

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
        } else {
            PendingSession newSession = pendingSessionRepository.save(new PendingSession(sendingPlayer.getId(), receivingPlayer.getId()));

            if (originalSession != null) {
                notificationService.sendRematchRequestNotification(receivingPlayer.getId(), sendingPlayer.getId(), sendingPlayer.getDisplayName(), newSession.getId());
            } else {
                notificationService.sendMatchRequestNotification(receivingPlayer.getId(), sendingPlayer.getId(), sendingPlayer.getDisplayName(), newSession.getId());
            }
        }
    }

    @Transactional
    public Long handleMatchAccept(MatchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND, "Pending session not found for: " + response.getPendingSessionId()));

        if (!pendingSession.getOpponentId().equals(response.getSenderId())) {
            throw new AccessDeniedException("Not authorized to accept this request");
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
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NOT_FOUND, "Pending session not found for: " + response.getPendingSessionId()));

        if (response.isRematch()) {
            notificationService.sendRematchRejectedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId());
        } else {
            notificationService.sendMatchRejectedNotification(response.getReceiverId(), response.getPlayerDisplayName(), response.getNotificationId());
        }
        pendingSessionRepository.delete(pendingSession);
    }
}
