package org.borg.backend.unit.social.friendship;

import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.FriendshipException;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.dto.PlayerInteraction;
import org.borg.backend.social.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.social.friendship.dto.RelationshipStatusRequest;
import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.social.friendship.model.FriendshipStatus;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.friendship.service.FriendshipService;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.social.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class FriendshipServiceTest {
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PlayerService playerService;
    @Mock
    private PlayerBlockService playerBlockService;
    @Mock
    private NotificationRepository notificationRepository;
    
    @InjectMocks
    private FriendshipService friendshipService;

    private Player player1;
    private Player player2;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player1 = Player.builder()
                .id(1L)
                .username("player1")
                .build();

        player2 = Player.builder()
                .id(2L)
                .username("player2")
                .build();
    }
    
    @Nested
    class SendFriendRequestTests {
        
        @Test
        void shouldThrowErrorWhenFriendingSelf() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player1.getId());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(friendRequest),
                    "Players should not be able to send friend requests to themselves");

            assertEquals(BusinessErrorCodes.CANNOT_FRIEND_SELF, exception.getErrorCode(),
                    "Self-friend request should be rejected with appropriate error code");
            
            verify(playerService, never()).getPlayerById(any());
        }
        
        @Test
        void shouldSilentlyReturnWhenBlockExists_AndSendHiddenNotification() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            
            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerBlockService.checkBlocksForFriendRequest(player1.getId(), player2.getId())).thenReturn(true);
            
            friendshipService.sendFriendRequest(friendRequest);
            
            verify(notificationService).sendFriendRequestNotification(player2.getId(), player1, true);
            verify(playerService, never()).getPlayerById(player2.getId());
        }
        
        @Test
        void shouldSendFriendRequest() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerBlockService.checkBlocksForFriendRequest(player1.getId(), player2.getId())).thenReturn(false);
            
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.empty());
            
            friendshipService.sendFriendRequest(friendRequest);

            ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
            verify(friendshipRepository).save(friendshipCaptor.capture());

            Friendship savedFriendship = friendshipCaptor.getValue();
            assertEquals(FriendshipStatus.PENDING, savedFriendship.getStatus());
            assertEquals(player1, savedFriendship.getPlayer1());
            assertEquals(player2, savedFriendship.getPlayer2());
        }
        
        @Test
        void shouldThrowErrorWhenPendingRequestAlreadyExists() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            Friendship existingFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.PENDING).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerBlockService.checkBlocksForFriendRequest(player1.getId(), player2.getId())).thenReturn(false);

            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);

            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));

            FriendshipException exception =  assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(friendRequest),
                    "Players should not be able to send friend requests when one is already pending");

            assertEquals(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING, exception.getErrorCode(),
                    "Sending friend request when one is already pending should be rejected with appropriate error code");
        }

        @Test
        void shouldThrowErrorWhenActiveFriendshipExists() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            Friendship existingFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.ACTIVE).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerBlockService.checkBlocksForFriendRequest(player1.getId(), player2.getId())).thenReturn(false);

            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);

            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));

            FriendshipException exception =  assertThrows(FriendshipException.class,
                    () -> friendshipService.sendFriendRequest(friendRequest),
                    "Player should not be able to send a friend request when an active friendship exists");

            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS, exception.getErrorCode(),
                    "Sending friend request when a friendship is already active should be rejected with appropriate error code");
        }

        @Test
        void shouldAutoAcceptFriendshipWhenReceiverSendsRequest() {
            PlayerInteraction friendRequest = new PlayerInteraction(player1.getId(), player2.getId());
            Friendship existingFriendship = Friendship.builder()
                    .player1(player2)
                    .player2(player1)
                    .status(FriendshipStatus.PENDING).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerBlockService.checkBlocksForFriendRequest(player1.getId(), player2.getId())).thenReturn(false);

            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);

            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));
            
            friendshipService.sendFriendRequest(friendRequest);

            ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
            verify(friendshipRepository).save(friendshipCaptor.capture());

            Friendship savedFriendship = friendshipCaptor.getValue();
            assertEquals(FriendshipStatus.ACTIVE, savedFriendship.getStatus());
            assertEquals(player2, savedFriendship.getPlayer1());
            assertEquals(player1, savedFriendship.getPlayer2());
            
            verify(notificationService).sendFriendAcceptedNotification(player1.getId(), player2);
            verify(notificationService).sendFriendAcceptedNotification(player2.getId(), player1);
            verify(notificationService).deleteFriendRequestByPlayerIds(player1.getId(), player2.getId());
        }
    }

    @Nested
    class HandleFriendshipResponseTests {

        @Test
        void shouldAcceptFriendRequestAndSendNotifications() {
            PlayerInteractionResponse response = new PlayerInteractionResponse(player1.getId(), player2.getId(), 1L);

            Friendship existingFriendship = Friendship.builder()
                    .player1(player2)
                    .player2(player1)
                    .status(FriendshipStatus.PENDING).build();
            
            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));
            
            friendshipService.handleFriendshipResponse(response, true);

            ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
            verify(friendshipRepository).save(friendshipCaptor.capture());
            
            Friendship savedFriendship = friendshipCaptor.getValue();
            assertEquals(FriendshipStatus.ACTIVE, savedFriendship.getStatus());
            assertEquals(player2, savedFriendship.getPlayer1());
            assertEquals(player1, savedFriendship.getPlayer2());
            
            verify(notificationService).sendFriendAcceptedNotification(player2.getId(), player1);
            
            verify(notificationRepository).deleteById(response.getNotificationId());
            
        }

        @Test
        void shouldRejectFriendRequestAndDeleteFriendship() {
            PlayerInteractionResponse response = new PlayerInteractionResponse(player1.getId(), player2.getId(), 1L);

            Friendship existingFriendship = Friendship.builder()
                    .player1(player2)
                    .player2(player1)
                    .status(FriendshipStatus.PENDING).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));

            friendshipService.handleFriendshipResponse(response, false);
            
            verify(friendshipRepository).delete(existingFriendship);
            verify(notificationRepository).deleteById(response.getNotificationId());
        }

        @Test
        void shouldThrowErrorWhenFriendshipDoesNotExist() {
            PlayerInteractionResponse response = new PlayerInteractionResponse(player1.getId(), player2.getId(), 1L);

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.empty());

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.handleFriendshipResponse(response, true),
                    "Players should not be able to accept a friend request that does not exist");

            assertEquals(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND, exception.getErrorCode(),
                    "Accepting non-existing friendship should be rejected with appropriate error code");

            verify(notificationRepository).deleteById(response.getNotificationId());
            verify(friendshipRepository, never()).save(any());
            verify(notificationService, never()).sendFriendAcceptedNotification(any(), any());
        }

        @Test
        void shouldThrowErrorWhenFriendshipIsAlreadyActive() {
            PlayerInteractionResponse response = new PlayerInteractionResponse(player1.getId(), player2.getId(), 1L);

            Friendship existingFriendship = Friendship.builder()
                    .player1(player2)
                    .player2(player1)
                    .status(FriendshipStatus.ACTIVE).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));

            FriendshipException exception = assertThrows(FriendshipException.class,
                    () -> friendshipService.handleFriendshipResponse(response, true),
                    "Players should not be able to accept a friend request when the friendship already exists");

            assertEquals(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS, exception.getErrorCode(),
                    "Accepting active existing friendship should be rejected with appropriate error code");

            verify(notificationRepository).deleteById(response.getNotificationId());
            verify(friendshipRepository, never()).save(any());
            verify(notificationService, never()).sendFriendAcceptedNotification(any(), any());
        }
    }

    @Nested
    class RemoveAsFriendTests {
        @Test
        void shouldRemoveActiveFriendship() {
            PlayerInteraction removeRequest = new PlayerInteraction(player1.getId(), player2.getId());
            
            Friendship existingFriendship = Friendship.builder()
                    .player1(player2)
                    .player2(player1)
                    .status(FriendshipStatus.ACTIVE).build();
            
            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));
            
            friendshipService.removeAsFriend(removeRequest);
            
            verify(friendshipRepository).delete(existingFriendship);
        }

        @Test
        void shouldRemovePendingFriendRequest() {
            PlayerInteraction removeRequest = new PlayerInteraction(player1.getId(), player2.getId());

            Friendship existingFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.PENDING).build();

            when(playerService.getPlayerById(player1.getId())).thenReturn(player1);
            when(playerService.getPlayerById(player2.getId())).thenReturn(player2);
            when(friendshipRepository.findExistingFriendshipByPlayers(player1, player2)).thenReturn(Optional.of(existingFriendship));

            friendshipService.removeAsFriend(removeRequest);

            verify(friendshipRepository).delete(existingFriendship);
            verify(notificationService).deleteFriendRequestByPlayerIds(player2.getId(), player1.getId());
        }
    }

    @Nested
    class GetFriendshipStatusTests {
        @Test
        void shouldReturnNoneWhenNoFriendshipExists() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player1.getId(), player2.getId());

            when(friendshipRepository.findFriendshipBetweenPlayerIds(player1.getId(), player2.getId()))
                    .thenReturn(Optional.empty());

            FriendshipStatus status = friendshipService.getFriendshipStatus(request);

            assertEquals(FriendshipStatus.NONE, status);
        }

        @Test
        void shouldReturnPendingForSender() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player1.getId(), player2.getId());

            Friendship pendingFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.PENDING)
                    .build();

            when(friendshipRepository.findFriendshipBetweenPlayerIds(player1.getId(), player2.getId()))
                    .thenReturn(Optional.of(pendingFriendship));

            FriendshipStatus status = friendshipService.getFriendshipStatus(request);

            assertEquals(FriendshipStatus.PENDING, status);
        }

        @Test
        void shouldReturnIncomingRequestForReceiver() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player2.getId(), player1.getId());

            Friendship pendingFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.PENDING)
                    .build();

            when(friendshipRepository.findFriendshipBetweenPlayerIds(player2.getId(), player1.getId()))
                    .thenReturn(Optional.of(pendingFriendship));

            FriendshipStatus status = friendshipService.getFriendshipStatus(request);

            assertEquals(FriendshipStatus.INCOMING_REQUEST, status);
        }

        @Test
        void shouldReturnActiveForBothPlayers() {
            RelationshipStatusRequest request = new RelationshipStatusRequest(player1.getId(), player2.getId());

            Friendship activeFriendship = Friendship.builder()
                    .player1(player1)
                    .player2(player2)
                    .status(FriendshipStatus.ACTIVE)
                    .build();

            when(friendshipRepository.findFriendshipBetweenPlayerIds(player1.getId(), player2.getId()))
                    .thenReturn(Optional.of(activeFriendship));

            FriendshipStatus status = friendshipService.getFriendshipStatus(request);

            assertEquals(FriendshipStatus.ACTIVE, status);
            
            RelationshipStatusRequest reverseRequest = new RelationshipStatusRequest(player2.getId(), player1.getId());
            when(friendshipRepository.findFriendshipBetweenPlayerIds(player2.getId(), player1.getId()))
                    .thenReturn(Optional.of(activeFriendship));

            FriendshipStatus reverseStatus = friendshipService.getFriendshipStatus(reverseRequest);
            assertEquals(FriendshipStatus.ACTIVE, reverseStatus);
        }
    }
}










