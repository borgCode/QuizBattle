package org.borg.backend.integration.multiplayer;


import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.game.multiplayer.dto.MatchRequest;
import org.borg.backend.game.multiplayer.dto.RematchRequest;
import org.borg.backend.game.multiplayer.dto.MatchResponse;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.PendingSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.game.multiplayer.service.PlayerMatchService;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PlayerMatchServiceIntegrationTest {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Autowired
    private PendingSessionRepository pendingSessionRepository;
    @Autowired
    private PlayerMatchService playerMatchService;
    @Autowired
    private PlayerRepository playerRepository;

    private Player sendingPlayer;
    private Player opponentPlayer;
    private MultiplayerSession completedMultiplayerSession;
    private final Pageable pageable = PageRequest.of(0, 15);

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

        completedMultiplayerSession = new MultiplayerSession(sendingPlayer, opponentPlayer, sendingPlayer.getId());
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
        MatchResponse matchResponse = setupRematchScenario();
        
        playerMatchService.handleMatchAccept(matchResponse);

        assertAll("Post-accept state checks",
                () -> assertNull(pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(opponentPlayer.getId(), pageable).isEmpty()),
                () -> assertTrue(multiplayerSessionRepository.checkIfOngoingSessionExists(
                        sendingPlayer.getId(), opponentPlayer.getId(), GameStatus.ACTIVE))
        );

        Page<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(sendingPlayer.getId(), pageable);
        Notification sendingPlayerNotification = sendingPlayerNotifications.getContent().get(0);
        assertEquals(NotificationType.REMATCH_ACCEPTED, sendingPlayerNotification.getType());
    }

    @Test
    void rematchRequestWhenNoRequestsHaveBeenSentAndReject() {
        MatchResponse matchResponse = setupRematchScenario();
        
        playerMatchService.handleMatchReject(matchResponse);

        assertAll("Post-reject state checks",
                () -> assertNull(pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(opponentPlayer.getId(), pageable).isEmpty()),
                () -> assertFalse(multiplayerSessionRepository.checkIfOngoingSessionExists(
                        sendingPlayer.getId(), opponentPlayer.getId(), GameStatus.ACTIVE))
        );

        Page<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(sendingPlayer.getId(), pageable);
        Notification sendingPlayerNotification = sendingPlayerNotifications.getContent().get(0);
        assertEquals(NotificationType.REMATCH_DECLINED, sendingPlayerNotification.getType());
    }
    
    private MatchResponse setupRematchScenario() {
        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        playerMatchService.requestRematch(rematchRequest);

        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), opponentPlayer.getId());
        assertNotNull(pendingSession);

        Page<Notification> opponentNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(opponentPlayer.getId(), pageable);
        Notification opponentNotification = opponentNotifications.getContent().get(0);
        assertAll("Opponent notification checks",
                () -> assertEquals(pendingSession.getId(), opponentNotification.getPendingSessionId()),
                () -> assertEquals(sendingPlayer.getDisplayName() + " requested a rematch against you!",
                        opponentNotification.getMessage()),
                () -> assertEquals(NotificationType.REMATCH_REQUEST, opponentNotification.getType())
        );

        return new MatchResponse(
                opponentPlayer.getId(),
                opponentNotification.getSenderId(),
                opponentPlayer.getDisplayName(),
                opponentNotification.getPendingSessionId(),
                opponentNotification.getId(),
                true
        );
    }

    @Test
    void rematchRequestWhenOpponentAlreadyRequested() {

        RematchRequest opponentRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), opponentPlayer.getId());
        playerMatchService.requestRematch(opponentRematchRequest);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        playerMatchService.requestRematch(rematchRequest);

        Page<Notification> opponentNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(opponentPlayer.getId(), pageable);
        Page<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(sendingPlayer.getId(), pageable);

        assertAll("Both players should only have rematch accepted notifications",
                () -> assertTrue(opponentNotifications.getTotalElements() == 1
                        && opponentNotifications.getContent().get(0).getType().equals(NotificationType.REMATCH_ACCEPTED)),
                () -> assertTrue(sendingPlayerNotifications.getTotalElements() == 1
                        && sendingPlayerNotifications.getContent().get(0).getType().equals(NotificationType.REMATCH_ACCEPTED))
        );

        assertAll("Previous pending states should be cleaned up",
                () -> assertFalse(pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(
                        sendingPlayer.getId(), opponentPlayer.getId())),
                () -> assertFalse(pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(
                        opponentPlayer.getId(), sendingPlayer.getId()))
        );

        assertTrue(multiplayerSessionRepository.checkIfOngoingSessionExists(
                sendingPlayer.getId(), opponentPlayer.getId(), GameStatus.ACTIVE));
    }

    @Test
    void shouldThrowErrorWhenSessionIsActive() {
        completedMultiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(completedMultiplayerSession);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> playerMatchService.requestRematch(rematchRequest));
        assertEquals(BusinessErrorCodes.GAME_ALREADY_ONGOING, exception.getErrorCode());
    }

    @Test
    void shouldThrowExceptionWhenAnotherGameIsOngoing() {
        MultiplayerSession ongoingSession = new MultiplayerSession(sendingPlayer, opponentPlayer, sendingPlayer.getId());
        ongoingSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(ongoingSession);

        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> playerMatchService.requestRematch(rematchRequest));
        assertEquals(BusinessErrorCodes.GAME_ALREADY_ONGOING, exception.getErrorCode());

    }

    @Test
    void shouldThrowExceptionWhenSendingMultipleRequests() {
        RematchRequest firstRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());
        playerMatchService.requestRematch(firstRematchRequest);

        RematchRequest secondRematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId());

        GameException exception = assertThrows(GameException.class,
                () -> playerMatchService.requestRematch(secondRematchRequest));

        assertEquals(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT, exception.getErrorCode());
    }

    @Test
    void regularMatchRequestShouldUseCorrectNotificationTypes() {
        MatchRequest matchRequest = new MatchRequest(sendingPlayer.getId(), opponentPlayer.getId());
        playerMatchService.requestMatch(matchRequest);
        
        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(
                sendingPlayer.getId(), opponentPlayer.getId());
        assertNotNull(pendingSession);

        Page<Notification> opponentNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(opponentPlayer.getId(), pageable);
        Notification opponentNotification = opponentNotifications.getContent().get(0);
        assertAll("Regular match notification checks",
                () -> assertEquals(pendingSession.getId(), opponentNotification.getPendingSessionId()),
                () -> assertEquals(sendingPlayer.getDisplayName() + " requested a match against you!",
                        opponentNotification.getMessage()),
                () -> assertEquals(NotificationType.MATCH_REQUEST, opponentNotification.getType())
        );
        
        MatchResponse response = new MatchResponse(
                opponentPlayer.getId(),
                opponentNotification.getSenderId(),
                opponentPlayer.getDisplayName(),
                opponentNotification.getPendingSessionId(),
                opponentNotification.getId(),
                false
        );
        playerMatchService.handleMatchAccept(response);

        Page<Notification> sendingPlayerNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(sendingPlayer.getId(), pageable);
        assertEquals(NotificationType.MATCH_ACCEPTED, sendingPlayerNotifications.getContent().get(0).getType());
    }
}


