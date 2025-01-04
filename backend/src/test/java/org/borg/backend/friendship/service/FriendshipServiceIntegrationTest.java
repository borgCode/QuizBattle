package org.borg.backend.friendship.service;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.FriendshipStatus;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.friendship.dto.PlayerInteraction;
import org.borg.backend.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.friendship.repository.FriendshipRepository;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FriendshipServiceIntegrationTest {

    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;


    private Player sender;
    private Player receiver;
    @Autowired
    private FriendshipService friendshipService;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        playerRepository.deleteAll();
        notificationRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        sender = createAndSavePlayer("sender");
        receiver = createAndSavePlayer("receiver");
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


    @Nested
    class FriendRequestTests {
        @Test
        void sendFriendRequestWithNoPendingRequestAndBeAccepted() {
            PlayerInteractionResponse response = setupFriendRequestScenario();
            
            friendshipService.handleFriendshipResponse(response, true);
            
            assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(receiver.getId()).isEmpty());

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(sender, receiver);

            assertAll("Post-accept friendship",
                    () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                    () -> assertEquals(sender, friendship.get().getPlayer1(), "Expected sender was not the actual sender"),
                    () -> assertEquals(receiver, friendship.get().getPlayer2(), "Expected receiver was not the actual receiver"),
                    () -> assertEquals(FriendshipStatus.ACTIVE, friendship.get().getStatus(), "Friendship should be PENDING")
            );

            List<Notification> notifications = notificationRepository.findByPlayerIdAndIsReadFalse(sender.getId());
            Notification friendRequestAcceptedNotification = notifications.get(0);
            assertEquals(NotificationType.FRIEND_ACCEPTED, friendRequestAcceptedNotification.getType());
        }

        private PlayerInteractionResponse setupFriendRequestScenario() {
            PlayerInteraction playerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            friendshipService.sendFriendRequest(playerInteraction);

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(sender, receiver);
            List<Notification> notifications = notificationRepository.findByPlayerIdAndIsReadFalse(receiver.getId());
            Notification friendRequestNotification = notifications.get(0);
            
            assertAll("Post-request friendship",
                    () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                    () -> assertEquals(sender, friendship.get().getPlayer1(), "Expected sender was not the actual sender"),
                    () -> assertEquals(receiver, friendship.get().getPlayer2(), "Expected receiver was not the actual receiver"),
                    () -> assertEquals(FriendshipStatus.PENDING, friendship.get().getStatus(), "Friendship should be PENDING"),
                    () -> assertEquals(NotificationType.FRIEND_REQUEST, friendRequestNotification.getType(), "Notification type was not FRIEND_REQUEST")
            );
            
            return new PlayerInteractionResponse(receiver.getId(), sender.getId(), friendRequestNotification.getId());
        }
    }

    
    
}