package org.borg.backend.friendship.service;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.FriendshipStatus;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.common.exceptions.FriendshipException;
import org.borg.backend.friendship.dto.PlayerInteraction;
import org.borg.backend.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.friendship.dto.RelationshipStatusRequest;
import org.borg.backend.friendship.dto.RelationshipsDTO;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.friendship.repository.FriendshipRepository;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
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

    private Player player1;
    private Player player2;

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

            assertAll("Post-accept friendship",
                    () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                    () -> assertEquals(player1, friendship.get().getPlayer1(), "Expected sender was not the actual sender"),
                    () -> assertEquals(player2, friendship.get().getPlayer2(), "Expected receiver was not the actual receiver"),
                    () -> assertEquals(FriendshipStatus.ACTIVE, friendship.get().getStatus(), "Friendship should be PENDING")
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

            assertFalse(friendshipRepository.findByPlayer1AndPlayer2OrPlayer1AndPlayer2(player1, player2, player2, player1).isEmpty());

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
                    .playerId(player2.getId())
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

        @Test
        void throwErrorWhenSendingToBlockedFriendship() {
            PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            friendshipService.blockPlayer(playerInteraction);

            PlayerInteraction otherPlayerInteraction = new PlayerInteraction(player2.getId(), player1.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(otherPlayerInteraction));

            assertEquals(BusinessErrorCodes.CANNOT_SENT_REQUEST_TO_BLOCKED_PLAYER, exception.getErrorCode());


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
    class BlockPlayerTests {

        @Test
        void successfullyBlockPlayer() {
            PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            friendshipService.blockPlayer(playerInteraction);

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
            assertAll("Post-block friendship",
                    () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                    () -> assertEquals(player1, friendship.get().getPlayer1(), "The player who blocked was not the expected player"),
                    () -> assertEquals(player2, friendship.get().getPlayer2(), "The blocked player was not the expected player"),
                    () -> assertEquals(FriendshipStatus.BLOCKED, friendship.get().getStatus(), "Friendship should be BLOCKED")
            );
        }
        @Test
        void blockingShouldRemoveFriendRequest() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(friendRequest);

            PlayerInteraction blockInteraction = new PlayerInteraction(player2.getId(), player1.getId());
            friendshipService.blockPlayer(blockInteraction);
            
            assertEquals(0, notificationRepository.findByPlayerId(player2.getId()).size(),
                    "Player 1 should have no notifications after blocking player2");
            
            
        }

        @Test
        void throwErrorWhenAlreadyBlocked() {
            PlayerInteraction firstPlayerInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            friendshipService.blockPlayer(firstPlayerInteraction);

            PlayerInteraction secondPlayerInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.blockPlayer(secondPlayerInteraction));

            assertEquals(BusinessErrorCodes.ALREADY_BLOCKED_FRIENDSHIP, exception.getErrorCode());

        }

        @Test
        void successfullyUnblockPlayer() {
            PlayerInteraction blockInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.blockPlayer(blockInteraction);

            PlayerInteraction unblockInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.unblockPlayer(unblockInteraction);

            Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
            assertFalse(friendship.isPresent(), "Relationship was not deleted from repo after unblock");

        }

        @Test
        void throwErrorWhenUnblockingNonBlockedPlayer() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(friendRequest);

            PlayerInteraction unblockInteraction = new PlayerInteraction(player1.getId(), player2.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.unblockPlayer(unblockInteraction));

            assertEquals(BusinessErrorCodes.CANNOT_UNBLOCK_ACTIVE_FRIENDSHIP, exception.getErrorCode());

        }
    }

    @Nested
    class RelationshipQueryTests {
        @Test
        void getFriendshipStatusTest() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player1.getId(), player2.getId());
            RelationshipStatusRequest receiverRequest = new RelationshipStatusRequest(player2.getId(), player1.getId());

            FriendshipStatus noneStatus = friendshipService.getRelationshipStatus(request);
            assertEquals(FriendshipStatus.NONE, noneStatus, "Status should be NONE");

            PlayerInteraction friendRequestFromPlayer1 = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.sendFriendRequest(friendRequestFromPlayer1);

            FriendshipStatus pendingStatus = friendshipService.getRelationshipStatus(request);
            assertEquals(FriendshipStatus.PENDING, pendingStatus, "Sender should see PENDING status");

            FriendshipStatus incomingStatus = friendshipService.getRelationshipStatus(receiverRequest);
            assertEquals(FriendshipStatus.INCOMING_REQUEST, incomingStatus, "Receiver should see INCOMING_REQUEST status");

            PlayerInteraction friendRequestFromPlayer2 = new PlayerInteraction(player2.getId(), player1.getId());
            friendshipService.sendFriendRequest(friendRequestFromPlayer2);

            FriendshipStatus activeStatus = friendshipService.getRelationshipStatus(request);
            assertEquals(FriendshipStatus.ACTIVE, activeStatus, "Status should be ACTIVE");

            PlayerInteraction blockRequestFromPlayer1 = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.blockPlayer(blockRequestFromPlayer1);

            FriendshipStatus blockedStatus = friendshipService.getRelationshipStatus(request);
            assertEquals(FriendshipStatus.BLOCKED, blockedStatus, "Blocker should see BLOCKED status");

            FriendshipStatus blockedPlayerStatus = friendshipService.getRelationshipStatus(receiverRequest);
            assertEquals(FriendshipStatus.NONE, blockedPlayerStatus, "Blocked player should see NONE status");
            
        }

        @Test
        void getBlockedPlayersOnlyReturnsPlayersBlockedByRequestingPlayer() {
            Player thirdPlayer = createAndSavePlayer("thirdPlayer");

            PlayerInteraction senderBlocksReceiver = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.blockPlayer(senderBlocksReceiver);

            PlayerInteraction thirdPlayerBlocksSender = new PlayerInteraction(thirdPlayer.getId(), player1.getId());
            friendshipService.blockPlayer(thirdPlayerBlocksSender);

            RelationshipsDTO senderRelationships = friendshipService.getRelationships(player1.getId());
            RelationshipsDTO receiverRelationships = friendshipService.getRelationships(player2.getId());
            RelationshipsDTO thirdPlayerRelationships = friendshipService.getRelationships(thirdPlayer.getId());

            assertAll("Blocked players visibility",
                    () -> assertEquals(1, senderRelationships.getBlocked().size(),
                            "Player1 should see one blocked player"),
                    () -> assertTrue(senderRelationships.getBlocked().stream()
                                    .anyMatch(blocked -> blocked.getId().equals(player2.getId())),
                            "Player1 should see player2 as blocked"),
                    () -> assertTrue(senderRelationships.getFriends().isEmpty(),
                            "Player1 should have no friends"),
                    
                    () -> assertTrue(receiverRelationships.getBlocked().isEmpty(),
                            "Player2 should see no blocked players"),
                    () -> assertTrue(receiverRelationships.getFriends().isEmpty(),
                            "Player2 should have no friends"),
                    
                    () -> assertEquals(1, thirdPlayerRelationships.getBlocked().size(),
                            "ThirdPlayer should see one blocked player"),
                    () -> assertTrue(thirdPlayerRelationships.getBlocked().stream()
                                    .anyMatch(blocked -> blocked.getId().equals(player1.getId())),
                            "ThirdPlayer should see player1 as blocked"),
                    () -> assertTrue(thirdPlayerRelationships.getFriends().isEmpty(),
                            "ThirdPlayer should have no friends")
            );
        }

        @Test
        void blockingActivePlayerRemovesBidirectionalFriendship() {
            PlayerInteractionResponse response = setupFriendRequestScenario();
            friendshipService.handleFriendshipResponse(response, true);
            
            PlayerInteraction blockInteraction = new PlayerInteraction(player1.getId(), player2.getId());
            friendshipService.blockPlayer(blockInteraction);
            
            RelationshipsDTO player1Relationships = friendshipService.getRelationships(player1.getId());
            RelationshipsDTO player2Relationships = friendshipService.getRelationships(player2.getId());

            assertAll("Post-block relationship state",
                    () -> assertTrue(player1Relationships.getFriends().isEmpty(), "Player1 should have no friends"),
                    () -> assertEquals(1, player1Relationships.getBlocked().size(), "Player1 should have one blocked player"),
                    () -> assertTrue(player1Relationships.getBlocked().stream()
                                    .anyMatch(blocked -> blocked.getId().equals(player2.getId())),
                            "Player1 should see Player2 as blocked"),
                    
                    () -> assertTrue(player2Relationships.getFriends().isEmpty(),
                            "Player2 should no longer see Player1 as friend"),
                    () -> assertTrue(player2Relationships.getBlocked().isEmpty(),
                            "Player2 should have no blocked players")
            );
        }

    }

    private PlayerInteractionResponse setupFriendRequestScenario() {
        PlayerInteraction playerInteraction = new PlayerInteraction(player1.getId(), player2.getId());
        friendshipService.sendFriendRequest(playerInteraction);

        Optional<Friendship> friendship = friendshipRepository.findByPlayer1AndPlayer2(player1, player2);
        
        
        List<Notification> notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId());
        Notification friendRequestNotification = notifications.get(0);

        assertAll("Post-request friendship",
                () -> assertTrue(friendship.isPresent(), "Friendship was not saved to repository"),
                () -> assertEquals(player1, friendship.get().getPlayer1(), "Expected sender was not the actual sender"),
                () -> assertEquals(player2, friendship.get().getPlayer2(), "Expected receiver was not the actual receiver"),
                () -> assertEquals(FriendshipStatus.PENDING, friendship.get().getStatus(), "Friendship should be PENDING"),
                () -> assertEquals(NotificationType.FRIEND_REQUEST, friendRequestNotification.getType(), "Notification type was not FRIEND_REQUEST")
        );

        return new PlayerInteractionResponse(player2.getId(), player1.getId(), friendRequestNotification.getId());
    }
}