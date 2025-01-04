package org.borg.backend.multiplayer.service;


import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.enums.NotificationType;
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
public class MultiplayerServiceIntegrationTest {
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
    }
   
    
    @Test
    void rematchRequest_acceptRequest_startSession() {

        Player sendingPlayer = createAndSavePlayer("Sending player");
        Player opponentPlayer = createAndSavePlayer("Opponent player");
        
        
        MultiplayerSession completedMultiplayerSession = new MultiplayerSession(sendingPlayer, opponentPlayer, sendingPlayer);
        completedMultiplayerSession.setStatus(GameStatus.COMPLETED);
        multiplayerSessionRepository.save(completedMultiplayerSession);
        
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

        RematchResponse rematchResponse = new RematchResponse(
                opponentNotification.getSenderId(),
                opponentPlayer.getDisplayName(),
                opponentNotification.getPendingSessionId(),
                opponentNotification.getId()
        );
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


}
