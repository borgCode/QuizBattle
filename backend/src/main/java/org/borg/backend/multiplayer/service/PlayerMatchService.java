package org.borg.backend.multiplayer.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.multiplayer.dto.MatchRequest;
import org.borg.backend.multiplayer.dto.MatchResponse;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerMatchService {

    private final NotificationService notificationService;
    private final PendingSessionRepository pendingSessionRepository;
    private final PlayerRepository playerRepository;
    private final MultiplayerSessionRepository multiplayerSessionRepository;


    @Transactional
    public void requestMatch(MatchRequest matchRequest) {
        Player sendingPlayer = playerRepository.findById(matchRequest.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));
        Player receivingPlayer = playerRepository.findById(matchRequest.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        handleMatchRequest(sendingPlayer, receivingPlayer, null);

    }

    @Transactional
    public void requestRematch(RematchRequest rematchRequest) {
        MultiplayerSession session = multiplayerSessionRepository.findById(rematchRequest.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        if (session.getStatus().equals(GameStatus.ACTIVE)) {
            throw new GameException(BusinessErrorCodes.GAME_ALREADY_ONGOING);
        }

        Long playerId = rematchRequest.getPlayerId();

        Player sendingPlayer = session.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player not found in session"));

        Player opponentPlayer = session.getPlayers().stream()
                .filter(player -> !player.equals(sendingPlayer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Opponent not found in session"));

        handleMatchRequest(sendingPlayer, opponentPlayer, session);

    }

    private void handleMatchRequest(Player sendingPlayer, Player receivingPlayer, MultiplayerSession originalSession) {
        if (multiplayerSessionRepository.checkIfOngoingSessionExists(sendingPlayer, receivingPlayer, GameStatus.ACTIVE)) {
            throw new GameException(BusinessErrorCodes.GAME_ALREADY_ONGOING);
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), receivingPlayer.getId())) {
            throw new GameException(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT);
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
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));
        Long newSessionId = createMultiplayerSession(pendingSession);

        if (response.isRematch()) {
            notificationService.sendRematchAcceptedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
        } else {
            notificationService.sendMatchAcceptedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
        }
        pendingSessionRepository.delete(pendingSession);
        return newSessionId;
    }


    private Long createMultiplayerSession(PendingSession pendingSession) {
        Player player1 = playerRepository.findById(pendingSession.getRequestingPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
        Player player2 = playerRepository.findById(pendingSession.getOpponentId())
                .orElseThrow(() -> new NoSuchElementException("Opponent not found"));

        //Randomly choose who starts

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = multiplayerSessionRepository.save(
                new MultiplayerSession(player1, player2, startingPlayer));
        return session.getId();

    }

    @Transactional
    public void handleMatchReject(MatchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        if (response.isRematch()) {
            notificationService.sendRematchRejectedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId());
        } else {
            notificationService.sendMatchRejectedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId());
        }
        pendingSessionRepository.delete(pendingSession);
    }
}
