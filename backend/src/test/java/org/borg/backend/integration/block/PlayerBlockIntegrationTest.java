package org.borg.backend.integration.block;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.dto.PlayerInteraction;
import org.borg.backend.social.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.friendship.service.FriendshipService;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
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
    @Autowired
    private FriendshipService friendshipService;
    @Autowired
    private FriendshipRepository friendshipRepository;

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
        Long blockerId = player1.getId();

        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.blockPlayer(blockerId, blockerId));
        assertAll("Post block self checks",
                () -> assertEquals(BusinessErrorCodes.CANNOT_BLOCK_SELF, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d attempted to block themselves", blockerId), exception.getMessage()));
    }

    @Test
    void shouldThrowError_WhenBlockingAlreadyBlocked() {
        Long blockerId = player1.getId();
        Long blockedId = player2.getId();

        playerBlockService.blockPlayer(blockerId, blockedId);

        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.blockPlayer(blockerId, blockedId));
        assertAll("Post block checks",
                () -> assertEquals(BusinessErrorCodes.ALREADY_BLOCKED, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d has already blocked player %d",
                        blockerId, blockedId), exception.getMessage()));
    }

    @Test
    void shouldDeleteExistingFriendships() {
        createFriendship(player1, player2);

        playerBlockService.blockPlayer(player1.getId(), player2.getId());

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    boolean existingFriendships = friendshipRepository
                            .existsByPlayer1AndPlayer2OrPlayer1AndPlayer2(player1, player2, player2, player1);
                    assertFalse(existingFriendships);
                });
    }

    @Test
    void shouldThrowError_WhenUnblockingSelf() {
        Long blockerId = player1.getId();

        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.unblockPlayer(blockerId, blockerId));
        assertAll("Post block self checks",
                () -> assertEquals(BusinessErrorCodes.CANNOT_UNBLOCK_SELF, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d attempted to unblock themselves", blockerId), exception.getMessage()));
    }

    @Test
    void shouldThrowError_WhenUnblockingNonBlocked() {
        Long blockerId = player1.getId();
        Long blockedId = player2.getId();

        BlockException exception = assertThrows(BlockException.class,
                () -> playerBlockService.unblockPlayer(blockerId, blockedId));
        assertAll("Post block checks",
                () -> assertEquals(BusinessErrorCodes.CANNOT_UNBLOCK_WHEN_NOT_BLOCKED, exception.getErrorCode()),
                () -> assertEquals(String.format("Player %d tried to unblock %d with no block active",
                        blockerId, blockedId), exception.getMessage()));
    }

    @Test
    void shouldNotPublishEvent_WhenOnlyOnePlayerUnblocks() {
        playerBlockService.blockPlayer(player1.getId(), player2.getId());
        playerBlockService.blockPlayer(player2.getId(), player1.getId());

        playerBlockService.unblockPlayer(player1.getId(), player2.getId());

        assertTrue(playerBlockRepository.existsByBlockerIdAndBlockedId(player2.getId(), player1.getId()),
                "Reverse block should still exist");
    }

    @Test
    void shouldPublishEvent_WhenBothPlayersUnblock() {
        playerBlockService.blockPlayer(player1.getId(), player2.getId());
        playerBlockService.blockPlayer(player2.getId(), player1.getId());

        playerBlockService.unblockPlayer(player1.getId(), player2.getId());
        playerBlockService.unblockPlayer(player2.getId(), player1.getId());

        assertFalse(playerBlockRepository.existsByBlockerIdAndBlockedId(player1.getId(), player2.getId()),
                "First block should be removed");
        assertFalse(playerBlockRepository.existsByBlockerIdAndBlockedId(player2.getId(), player1.getId()),
                "Second block should be removed");
    }

    @Test
    void shouldHideFriendRequest_WhenBlocked() {
        playerBlockService.blockPlayer(player1.getId(), player2.getId());
        
        PlayerInteraction blockedRequest = new PlayerInteraction(player2.getId(), player1.getId());
        friendshipService.sendFriendRequest(blockedRequest);
        
        List<Friendship> friendships = friendshipRepository.findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                player1, player2, player2, player1);
        assertTrue(friendships.isEmpty(), "No friendship should be created when blocked");

        
        List<Notification> notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player1.getId());
        assertEquals(1, notifications.size(), "Should have one notification");
        Notification hiddenNotification = notifications.get(0);
        assertAll("Hidden notification properties",
                () -> assertEquals(NotificationType.FRIEND_REQUEST, hiddenNotification.getType()),
                () -> assertTrue(hiddenNotification.isHiddenByBlock()),
                () -> assertEquals(player2.getId(), hiddenNotification.getSenderId()),
                () -> assertEquals(player1.getId(), hiddenNotification.getPlayerId())
        );
    }
    

    @Test
    void getBlockedPlayersOnlyReturnsPlayersBlockedByRequestingPlayer() {
        Player thirdPlayer = createAndSavePlayer("thirdPlayer");
        
        Long player1Id = player1.getId();
        Long player2Id = player2.getId();
        
        playerBlockService.blockPlayer(player1Id, player2Id);
        
        playerBlockService.blockPlayer(thirdPlayer.getId(), player1Id);

        List<PlayerDTO> senderBlocked = playerBlockService.getBlocked(player1.getId());
        List<PlayerDTO> receiverBlocked = playerBlockService.getBlocked(player2.getId());
        List<PlayerDTO> thirdBlocked = playerBlockService.getBlocked(thirdPlayer.getId());

        assertAll("Blocked players visibility",
                () -> assertEquals(1, senderBlocked.size(),
                        "Player1 should see one blocked player"),
                () -> assertTrue(senderBlocked.stream()
                                .anyMatch(blocked -> blocked.getId().equals(player2.getId())),
                        "Player1 should see player2 as blocked"),
                () -> assertTrue(receiverBlocked.isEmpty(),
                        "Player2 should see no blocked players"),
                () -> assertEquals(1, thirdBlocked.size(),
                        "ThirdPlayer should see one blocked player"),
                () -> assertTrue(thirdBlocked.stream()
                                .anyMatch(blocked -> blocked.getId().equals(player1.getId())),
                        "ThirdPlayer should see player1 as blocked")
        );
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

    private void createFriendship(Player player1, Player player2) {
        PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
        friendshipService.sendFriendRequest(playerInteraction);

        friendshipService.handleFriendshipResponse(new PlayerInteractionResponse(player2.getId(), player1.getId(), 1L), true);
    }
}
