package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.dto.RematchResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        if (multiplayerSessionRepository.checkIfOngoingSessionExists(sendingPlayer, opponentPlayer, GameStatus.ACTIVE)) {
            throw new GameException(BusinessErrorCodes.GAME_ALREADY_ONGOING);
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), opponentPlayer.getId())) {
            log.warn("Sending player id: {} opponent id: {}", sendingPlayer.getId(), opponentPlayer.getId());
            throw new GameException(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT);
        }
        log.warn("Creating pending session");

        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(opponentPlayer.getId(), sendingPlayer.getId());
        log.warn("Pending session is: " + pendingSession);

        if (pendingSession != null) {

            log.warn("Has pending session");

            Long newSessionId = createRematchSession(session);
            log.warn("new session id: {}", newSessionId);


            //Delete original notification
            notificationService.deleteMatchRequestNotification(playerId, pendingSession.getId());

            pendingSessionRepository.delete(pendingSession);


            notificationService.sendRematchStartedNotification(playerId, opponentPlayer.getDisplayName(), newSessionId);
            notificationService.sendRematchStartedNotification(opponentPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);


        } else {
            log.warn("No existing session, creating new pending");

            PendingSession newSession = pendingSessionRepository.save(new PendingSession(playerId, opponentPlayer.getId()));
            notificationService.sendRematchRequestNotification(opponentPlayer.getId(), sendingPlayer.getId(), sendingPlayer.getDisplayName(), newSession.getId());
        }
    }

    private Long createRematchSession(MultiplayerSession session) {
        List<Player> players = session.getPlayers();

        Player startingPlayer = Math.random() < 0.5 ? players.get(0) : players.get(1);


        MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(
                new MultiplayerSession(players.get(0), players.get(1), startingPlayer));
        return multiplayerSession.getId();
    }

    @Transactional
    public Long handleRematchAccept(RematchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));
        Long newSessionId = createMultiplayerSession(pendingSession);

        notificationService.sendRematchAcceptedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);

        pendingSessionRepository.delete(pendingSession);
        return newSessionId;
    }

    @Transactional
    public void handleRematchReject(RematchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        notificationService.sendRematchRejectedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId());
        pendingSessionRepository.delete(pendingSession);
        
        log.warn("Rematch rejected");
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
}
