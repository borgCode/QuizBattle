package org.borg.backend.integration.block;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.dto.BlockRequest;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PlayerBlockIntegrationTest {

    @Autowired
    private PlayerBlockRepository playerBlockRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    private Player player1;
    private Player player2;
    @Autowired
    private PlayerBlockService playerBlockService;

    @BeforeEach
    void setUp() {
        playerBlockRepository.deleteAll();
        playerRepository.deleteAll();
        notificationRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        player1 = createAndSavePlayer("sender");
        player2 = createAndSavePlayer("receiver");
    }

    @Test
    void shouldThrowError_WhenBlockingSelf() {
        BlockRequest blockRequest = new BlockRequest(player1.getId(), player1.getId());
        
        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.blockPlayer(blockRequest));
        assertAll("Post block self checks",
                () -> assertEquals(BusinessErrorCodes.CANNOT_BLOCK_SELF, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d attempted to block themselves", blockRequest.getSenderId()), exception.getMessage()));
    }

    @Test
    void shouldThrowError_WhenBlockingAlreadyBlocked() {
        BlockRequest blockRequest = new BlockRequest(player1.getId(), player2.getId());

        playerBlockService.blockPlayer(blockRequest);

        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.blockPlayer(blockRequest));
        assertAll("Post block checks",
                () -> assertEquals(BusinessErrorCodes.ALREADY_BLOCKED, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d has already blocked player %d",
                        blockRequest.getSenderId(), blockRequest.getReceiverId()), exception.getMessage()));
    }

    private Player createAndSavePlayer(String playerName) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username(playerName.toLowerCase())
                .password("password")
                .displayName(playerName)
                .accountLocked(false)
                .enabled(true)
                .roles(new ArrayList<>(List.of(userRole)))
                .build();

        return playerRepository.save(player);
    }
}
