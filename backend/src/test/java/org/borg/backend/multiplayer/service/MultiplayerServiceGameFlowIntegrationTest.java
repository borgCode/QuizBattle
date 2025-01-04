package org.borg.backend.multiplayer.service;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MultiplayerServiceGameFlowIntegrationTest {

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

    private Player player1;
    private Player player2;
    private MultiplayerSession multiplayerSession;

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

        player1 = createAndSavePlayer("player1");
        player2 = createAndSavePlayer("player2");

        multiplayerSession = new MultiplayerSession(player1, player2, player1);
        multiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(multiplayerSession);
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
