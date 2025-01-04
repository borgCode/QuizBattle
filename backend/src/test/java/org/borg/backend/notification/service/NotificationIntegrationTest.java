package org.borg.backend.notification.service;


import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.service.MultiplayerService;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationIntegrationTest {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
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
        
//        RematchRequest rematchRequest = new RematchRequest(completedMultiplayerSession.getId(), sendingPlayer.getId(), )
//
//        notificationService.sendRematchRequestNotification(opponentPlayer.getId(), sendingPlayer.getDisplayName(), );
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
