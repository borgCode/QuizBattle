package org.borg.backend.friendship.service;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.FriendshipStatus;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.common.exceptions.FriendshipException;
import org.borg.backend.common.exceptions.GameException;
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

import java.time.LocalDateTime;
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

    @Autowired
    private FriendshipService friendshipService;

    private Player sender;
    private Player receiver;

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

        @Test
        void sendFriendRequestWithNoPendingRequestAndBeRejected() {
            PlayerInteractionResponse response = setupFriendRequestScenario();

            friendshipService.handleFriendshipResponse(response, false);

            assertAll("Verify friend request rejection",
                    () -> assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(receiver.getId()).isEmpty()),
                    () -> assertTrue(friendshipRepository.findByPlayer1AndPlayer2(sender, receiver).isEmpty(), "Friendship was not deleted"),
                    () -> assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(sender.getId()).isEmpty())
            );
        }
        @Test
        void throwErrorWhenSendingRequestToExistingFriendship() {
            PlayerInteractionResponse response = setupFriendRequestScenario();
            
            friendshipService.handleFriendshipResponse(response, true);
            
            PlayerInteraction newFriendRequest = new PlayerInteraction(sender.getId(), receiver.getId());
            
            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(newFriendRequest));
            
            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS, exception.getErrorCode());
            
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
        @Test
        void automaticallyAcceptFriendshipWhenBothPlayersSendRequest() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            friendshipService.sendFriendRequest(firstPlayerInteraction);

            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(receiver.getId(), sender.getId());
            friendshipService.sendFriendRequest(secondPlayerInteraction);
            
            List<Notification> notificationsPlayer1 = notificationRepository.findByPlayerIdAndIsReadFalse(sender.getId());
            List<Notification> notificationsPlayer2 = notificationRepository.findByPlayerIdAndIsReadFalse(receiver.getId());
            
            assertAll("Both players should only have friend request accepted notifications",
                    () -> assertTrue(notificationsPlayer1.size() == 1
                    && notificationsPlayer1.get(0).getType().equals(NotificationType.FRIEND_ACCEPTED)),
            () -> assertTrue(notificationsPlayer2.size() == 1
                    && notificationsPlayer2.get(0).getType().equals(NotificationType.FRIEND_ACCEPTED))
            );

            assertFalse(friendshipRepository.findByPlayer1AndPlayer2OrPlayer1AndPlayer2(sender, receiver, receiver, sender).isEmpty());
            
        }
        @Test
        void throwErrorWhenSendingSendingMultipleRequests() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            friendshipService.sendFriendRequest(firstPlayerInteraction);
            
            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            
            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(secondPlayerInteraction)
            );
            
            assertEquals(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING, exception.getErrorCode());
        }
        
        @Test
        void throwErrorWhenRespondingToNoExistingFriendRequest() {
            
            Notification friendRequest = Notification.builder()
                    .playerId(receiver.getId())
                    .senderId(sender.getId())
                    .type(NotificationType.FRIEND_REQUEST)
                    .message(sender.getDisplayName() + " sent you a friend request!")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Notification savedNotification = notificationRepository.save(friendRequest);
            
            PlayerInteractionResponse response = new PlayerInteractionResponse(receiver.getId(), sender.getId(), savedNotification.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.handleFriendshipResponse(response, true));
            
            assertEquals(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND, exception.getErrorCode());
            
            assertTrue(notificationRepository.findByPlayerIdAndIsReadFalse(receiver.getId()).isEmpty());
            
        }
        @Test
        void throwErrorWhenSendingToBlockedFriendship() {
            PlayerInteraction playerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            
            friendshipService.blockPlayer(playerInteraction);
            
            PlayerInteraction otherPlayerInteraction = new PlayerInteraction(receiver.getId(), sender.getId());
            
            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(otherPlayerInteraction));
            
            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED, exception.getErrorCode());
            
            
        }
        
    }
    @Nested
    class BlockPlayerTests {
        
        @Test
        void successfullyBlockPlayer() {
            PlayerInteraction playerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());

            friendshipService.blockPlayer(playerInteraction);

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(sender, receiver);
            assertAll("Post-block friendship",
                    () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                    () -> assertEquals(sender, friendship.get().getPlayer1(), "The player who blocked was not the expected player"),
                    () -> assertEquals(receiver, friendship.get().getPlayer2(), "The blocked player was not the expected player"),
                    () -> assertEquals(FriendshipStatus.BLOCKED, friendship.get().getStatus(), "Friendship should be BLOCKED")
            );
        }
        
        @Test
        void throwErrorWhenAlreadyBlocked() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());

            friendshipService.blockPlayer(firstPlayerInteraction);

            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(sender.getId(), receiver.getId());
            
            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.blockPlayer(secondPlayerInteraction));
            
            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED, exception.getErrorCode());

        }
        
    }

    
    
}