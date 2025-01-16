package org.borg.backend.integration.friendship;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.service.FriendshipService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.social.friendship.model.FriendshipStatus;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.shared.exceptions.FriendshipException;
import org.borg.backend.social.friendship.dto.PlayerInteraction;
import org.borg.backend.social.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.social.friendship.dto.RelationshipStatusRequest;
import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private Player player1;
    private Player player2;
    @Autowired
    private PlayerBlockService playerBlockService;

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

        player1 = createAndSavePlayer("sender");
        player2 = createAndSavePlayer("receiver");
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

            assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId()).isEmpty());

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
            assertTrue(friendship.isPresent(), "Friendship was not saved to repository");

            Friendship actualFriendship = friendship.get();
            assertAll("Post-accept friendship",
                    () -> assertEquals(player1, actualFriendship.getPlayer1(), "Expected sender was not the actual sender"),
                    () -> assertEquals(player2, actualFriendship.getPlayer2(), "Expected receiver was not the actual receiver"),
                    () -> assertEquals(FriendshipStatus.ACTIVE, actualFriendship.getStatus(), "Friendship should be ACTIVE")
            );

            List<Notification> notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player1.getId());
            Notification friendRequestAcceptedNotification = notifications.get(0);
            assertEquals(NotificationType.FRIEND_ACCEPTED, friendRequestAcceptedNotification.getType());
        }

        @Test
        void sendFriendRequestWithNoPendingRequestAndBeRejected() {
            PlayerInteractionResponse response = setupFriendRequestScenario();

            friendshipService.handleFriendshipResponse(response, false);

            assertAll("Verify friend request rejection",
                    () -> assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId()).isEmpty()),
                    () -> assertTrue(friendshipRepository.findByPlayer1AndPlayer2(player1, player2).isEmpty(), "Friendship was not deleted"),
                    () -> assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(player1.getId()).isEmpty())
            );
        }

        @Test
        void throwErrorWhenSendingRequestToExistingFriendship() {
            PlayerInteractionResponse response = setupFriendRequestScenario();

            friendshipService.handleFriendshipResponse(response, true);

            PlayerInteraction newFriendRequest = new PlayerInteraction(player1.getId(), player2.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(newFriendRequest));

            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS, exception.getErrorCode());
        }

        @Test
        void automaticallyAcceptFriendshipWhenBothPlayersSendRequest() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(firstPlayerInteraction);

            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(player2.getId(), player1.getId());
            friendshipService.sendFriendRequest(secondPlayerInteraction);

            List<Notification> notificationsPlayer1 = notificationRepository.findByPlayerIdAndIsArchivedFalse(player1.getId());
            List<Notification> notificationsPlayer2 = notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId());

            assertAll("Both players should only have friend request accepted notifications",
                    () -> assertTrue(notificationsPlayer1.size() == 1
                            && notificationsPlayer1.get(0).getType().equals(NotificationType.FRIEND_ACCEPTED)),
                    () -> assertTrue(notificationsPlayer2.size() == 1
                            && notificationsPlayer2.get(0).getType().equals(NotificationType.FRIEND_ACCEPTED))
            );

            assertFalse(friendshipRepository.findExistingFriendshipByPlayers(player1, player2).isEmpty());
        }

        @Test
        void throwErrorWhenSendingSendingMultipleRequests() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(firstPlayerInteraction);

            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(secondPlayerInteraction)
            );

            assertEquals(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING, exception.getErrorCode());
        }

        @Test
        void throwErrorWhenRespondingToNoExistingFriendRequest() {

            Notification friendRequest = Notification.builder()
                    .recipientId(player2.getId())
                    .senderId(player1.getId())
                    .type(NotificationType.FRIEND_REQUEST)
                    .message(player1.getDisplayName() + " sent you a friend request!")
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();

            Notification savedNotification = notificationRepository.save(friendRequest);

            PlayerInteractionResponse response = new PlayerInteractionResponse(player2.getId(), player1.getId(), savedNotification.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.handleFriendshipResponse(response, true));

            assertEquals(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND, exception.getErrorCode());

            assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId()).isEmpty());
        }
    }

    @Nested
    class removeFriendTests {
        @Test
        void removeActiveFriendTest() {
            PlayerInteractionResponse response = setupFriendRequestScenario();

            friendshipService.handleFriendshipResponse(response, true);

            friendshipService.removeAsFriend(new PlayerInteraction(player1.getId(), player2.getId()));

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
            assertFalse(friendship.isPresent(), "Relationship was not deleted from repo after removing friend");
        }

        @Test
        void removePendingFriendTest() {
            PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(playerInteraction);

            friendshipService.removeAsFriend(new PlayerInteraction(player1.getId(), player2.getId()));

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
            assertFalse(friendship.isPresent(), "Relationship was not deleted from repo after removing friend");

            assertTrue(notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId()).isEmpty());
        }
    }
    
    @Nested
    class RelationshipQueryTests {
        @Test
        void getFriendshipStatusTest() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player1.getId(), player2.getId());
            RelationshipStatusRequest receiverRequest = new RelationshipStatusRequest(player2.getId(), player1.getId());

            FriendshipStatus noneStatus = friendshipService.getFriendshipStatus(request);
            assertEquals(FriendshipStatus.NONE, noneStatus, "Status should be NONE");

            PlayerInteraction friendRequestFromPlayer1 = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(friendRequestFromPlayer1);

            FriendshipStatus pendingStatus = friendshipService.getFriendshipStatus(request);
            assertEquals(FriendshipStatus.PENDING, pendingStatus, "Sender should see PENDING status");

            FriendshipStatus incomingStatus = friendshipService.getFriendshipStatus(receiverRequest);
            assertEquals(FriendshipStatus.INCOMING_REQUEST, incomingStatus, "Receiver should see INCOMING_REQUEST status");

            PlayerInteraction friendRequestFromPlayer2 = new PlayerInteraction(player2.getId(), player1.getId());
            friendshipService.sendFriendRequest(friendRequestFromPlayer2);

            FriendshipStatus activeStatus = friendshipService.getFriendshipStatus(request);
            assertEquals(FriendshipStatus.ACTIVE, activeStatus, "Status should be ACTIVE");
        }
    }

    private PlayerInteractionResponse setupFriendRequestScenario() {
        PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
        friendshipService.sendFriendRequest(playerInteraction);

        Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);

        List<Notification> notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId());
        Notification friendRequestNotification = notifications.get(0);

        assertTrue(friendship.isPresent(), "Friendship was not saved to repository");

        Friendship actualFriendship = friendship.get();
        assertAll("Post-request friendship",
                () -> assertEquals(player1, actualFriendship.getPlayer1(), "Expected sender was not the actual sender"),
                () -> assertEquals(player2, actualFriendship.getPlayer2(), "Expected receiver was not the actual receiver"),
                () -> assertEquals(FriendshipStatus.PENDING, actualFriendship.getStatus(), "Friendship should be PENDING"),
                () -> assertEquals(NotificationType.FRIEND_REQUEST, friendRequestNotification.getType(), "Notification type was not FRIEND_REQUEST")
        );

        return new PlayerInteractionResponse(player2.getId(), player1.getId(), friendRequestNotification.getId());
    }
}