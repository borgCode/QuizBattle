package org.borg.backend.multiplayer.service;


import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.dto.RematchResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MultiplayerServiceRematchIntegrationTest {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Autowired
    private PendingSessionRepository pendingSessionRepository;
    @Autowired
    private MultiplayerService multiplayerService;
    @Autowired
    private PlayerRepository playerRepository;

    private Player sendingPlayer;
    private Player opponentPlayer;
    private MultiplayerSession completedMultiplayerSession;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        multiplayerSessionRepository.deleteAll();
        playerRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        sendingPlayer = createAndSavePlayer("Sender");
        opponentPlayer = createAndSavePlayer("Opponent");

        completedMultiplayerSession = new MultiplayerSession(sendingPlayer, opponentPlayer, sendingPlayer);
        completedMultiplayerSession.setStatus(GameStatus.COMPLETED);
        multiplayerSessionRepository.save(completedMultiplayerSession);
    }


    private Player createAndSavePlayer(String name) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username(name.toLowerCase())
                .password("password")
                .displayName(name)
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        return playerRepository.save(player);
    }

    @Test
    void rematchRequestWhenNoRequestsHaveBeenSentAndAccept() {
        RematchResponse rematchResponse = setupRematchScenario();
        
        multiplayerService.handleRematchAccept(rematchResponse);

        assertAll("Post-accept state checks",
                () -> assertNull(pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(opponentPlayer.getId()).isEmpty()),
                () -> assertTrue(multiplayerSessionRepository.checkIfOngoingSessionExists(
                        sendingPlayer, opponentPlayer, GameStatus.ACTIVE))
        );

        List<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsReadFalse(sendingPlayer.getId());
        Notification sendingPlayerNotification = sendingPlayerNotifications.get(0);
        assertEquals(NotificationType.REMATCH_ACCEPTED, sendingPlayerNotification.getType());

    }

    @Test
    void rematchRequestWhenNoRequestsHaveBeenSentAndReject() {
        RematchResponse rematchResponse = setupRematchScenario();
        
        multiplayerService.handleRematchReject(rematchResponse);

        assertAll("Post-reject state checks",
                () -> assertNull(pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(opponentPlayer.getId()).isEmpty()),
                () -> assertFalse(multiplayerSessionRepository.checkIfOngoingSessionExists(
                        sendingPlayer, opponentPlayer, GameStatus.ACTIVE))
        );

        List<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsReadFalse(sendingPlayer.getId());
        Notification sendingPlayerNotification = sendingPlayerNotifications.get(0);
        assertEquals(NotificationType.REMATCH_DECLINED, sendingPlayerNotification.getType());
    }
    
    private RematchResponse setupRematchScenario() {
        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        multiplayerService.requestRematch(rematchRequest);

        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), opponentPlayer.getId());
        assertNotNull(pendingSession);

        List<Notification> opponentNotifications = notificationRepository.findByPlayerIdAndIsReadFalse(opponentPlayer.getId());
        Notification opponentNotification = opponentNotifications.get(0);
        assertAll("Opponent notification checks",
                () -> assertEquals(pendingSession.getId(), opponentNotification.getPendingSessionId()),
                () -> assertEquals(sendingPlayer.getDisplayName() + " requested a rematch against you!",
                        opponentNotification.getMessage()),
                () -> assertEquals(NotificationType.REMATCH_REQUEST, opponentNotification.getType())
        );

        return new RematchResponse(
                opponentNotification.getSenderId(),
                opponentPlayer.getDisplayName(),
                opponentNotification.getPendingSessionId(),
                opponentNotification.getId()
        );
    }

    @Test
    void rematchRequestWhenOpponentAlreadyRequested() {

        RematchRequest opponentRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), opponentPlayer.getId());
        multiplayerService.requestRematch(opponentRematchRequest);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        multiplayerService.requestRematch(rematchRequest);

        List<Notification> opponentNotifications = notificationRepository.findByPlayerIdAndIsReadFalse(opponentPlayer.getId());
        List<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsReadFalse(sendingPlayer.getId());

        assertAll("Both players should only have rematch accepted notifications",
                () -> assertTrue(opponentNotifications.size() == 1
                        && opponentNotifications.get(0).getType().equals(NotificationType.REMATCH_ACCEPTED)),
                () -> assertTrue(sendingPlayerNotifications.size() == 1
                        && sendingPlayerNotifications.get(0).getType().equals(NotificationType.REMATCH_ACCEPTED))
        );

        assertAll("Previous pending states should be cleaned up",
                () -> assertFalse(pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertFalse(pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(
                        opponentPlayer.getId(), sendingPlayer.getId()))
        );

        assertTrue(multiplayerSessionRepository.checkIfOngoingSessionExists(
                sendingPlayer, opponentPlayer, GameStatus.ACTIVE));

    }

    @Test
    void shouldThrowErrorWhenSessionIsActive() {
        completedMultiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(completedMultiplayerSession);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> multiplayerService.requestRematch(rematchRequest));
        assertEquals(BusinessErrorCodes.GAME_ALREADY_ONGOING, exception.getErrorCode());
    }

    @Test
    void shouldThrowExceptionWhenAnotherGameIsOngoing() {
        MultiplayerSession ongoingSession = new MultiplayerSession(sendingPlayer, opponentPlayer, sendingPlayer);
        ongoingSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(ongoingSession);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> multiplayerService.requestRematch(rematchRequest));
        assertEquals(BusinessErrorCodes.GAME_ALREADY_ONGOING, exception.getErrorCode());

    }

    @Test
    void shouldThrowExceptionWhenSendingMultipleRequests() {
        RematchRequest firstRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        multiplayerService.requestRematch(firstRematchRequest);

        RematchRequest secondRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> multiplayerService.requestRematch(secondRematchRequest));

        assertEquals(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT, exception.getErrorCode());
    }

   
}


