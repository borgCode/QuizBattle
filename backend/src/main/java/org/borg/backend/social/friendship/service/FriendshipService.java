package org.borg.backend.social.friendship.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
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
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.social.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final PlayerBlockService playerBlockService;

    @Transactional
    public void sendFriendRequest(PlayerInteraction request) {
        log.info("Entering sendFriendRequest: senderId = {}, receiverId = {}", request.getSenderId(), request.getReceiverId());

        if (request.getSenderId().equals(request.getReceiverId())) {
            log.warn("Player {} attempted to send friend request to themselves", request.getSenderId());
            throw new FriendshipException(BusinessErrorCodes.CANNOT_FRIEND_SELF,
                    String.format("Player %d attempted to send friend request to themselves", request.getSenderId()));
        }
        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();

        Player sendingPlayer = playerService.getPlayerById(request.getSenderId());

        if (playerBlockService.checkBlocksForFriendRequest(senderId, receiverId)) {
            notificationService.sendFriendRequestNotification(receiverId, sendingPlayer, true);
            return;
        }
        log.info("No existing blocks between Player {} and Player {}", senderId, receiverId);

        Player receivingPlayer = playerService.getPlayerById(request.getReceiverId());

        Optional<Friendship> existingFriendship = friendshipRepository.
                findExistingFriendshipByPlayers(
                        sendingPlayer, receivingPlayer);

        if (existingFriendship.isEmpty()) {
            log.info("No existing friendship found, creating new friend request");
            friendshipRepository.save(Friendship.builder()
                    .player1(sendingPlayer)
                    .player2(receivingPlayer)
                    .friendShipDate(LocalDate.now())
                    .status(FriendshipStatus.PENDING)
                    .build());

            notificationService.sendFriendRequestNotification(receiverId, sendingPlayer, false);
            return;
        }

        Friendship friendship = existingFriendship.get();
        log.debug("Existing friendship found with status: {}", friendship.getStatus());

        if (friendship.getPlayer1().equals(sendingPlayer)) {
            switch (friendship.getStatus()) {
                case PENDING:
                    log.warn("Friend request already pending from player {} to player {}", senderId, receiverId);
                    throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING,
                            String.format("Friend request from player %d to player %d is already pending",
                                    senderId, receiverId));
                case ACTIVE:
                    throwAlreadyExistsException(senderId, receiverId);
            }
        } else if (friendship.getPlayer2().equals(sendingPlayer)) {
            switch (friendship.getStatus()) {
                case ACTIVE:
                    throwAlreadyExistsException(senderId, receiverId);
                case PENDING:
                    log.info("Friend request accepted by player {}, updating status to ACTIVE", senderId);
                    friendship.setStatus(FriendshipStatus.ACTIVE);
                    friendshipRepository.save(friendship);
                    notificationService.sendFriendAcceptedNotification(friendship.getPlayer1().getId(), sendingPlayer);
                    notificationService.sendFriendAcceptedNotification(friendship.getPlayer2().getId(), receivingPlayer);

                    notificationService.deleteFriendRequestByPlayerIds(senderId, receiverId);
            }
        }
    }

    private void throwAlreadyExistsException(Long senderId, Long receiverId) {
        log.warn("Friendship already exists between players {} and {}", senderId, receiverId);
        throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS,
                String.format("Friendship already exists between players %d and %d",
                        senderId, receiverId));
    }

    @Transactional
    public void handleFriendshipResponse(PlayerInteractionResponse response, boolean wantsFriendship) {
        log.info("Entering handleFriendshipResponse: senderId = {}, receiverId = {}, wantsFriendship = {}",
                response.getSenderId(), response.getReceiverId(), wantsFriendship);

        Long senderId = response.getSenderId();
        Long receiverId = response.getReceiverId();

        Player sendingPlayer = playerService.getPlayerById(senderId);
        Player receivingPlayer = playerService.getPlayerById(receiverId);

        Optional<Friendship> existingFriendship = friendshipRepository.
                findExistingFriendshipByPlayers(
                        sendingPlayer, receivingPlayer);

        if (existingFriendship.isEmpty()) {
            log.warn("No friendship found between players {} and {}, deleting notification", senderId, receiverId);
            notificationRepository.deleteById(response.getNotificationId());
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND,
                    String.format("No friendship found between players %d and %d",
                            senderId, receiverId));
        }

        Friendship friendship = existingFriendship.get();

        if (friendship.getStatus().equals(FriendshipStatus.ACTIVE)) {
            log.warn("Friendship already exists between players {} and {}, cannot respond", senderId, receiverId);
            notificationRepository.deleteById(response.getNotificationId());
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS,
                    String.format("Friendship already exists between players %d and %d",
                            senderId, receiverId));
        }
        log.debug("Updating friendship status based on player response");

        if (wantsFriendship) {
            friendship.setStatus(FriendshipStatus.ACTIVE);
            friendshipRepository.save(friendship);
            notificationService.sendFriendAcceptedNotification(response.getReceiverId(), sendingPlayer);
            log.info("Friendship accepted between players {} and {}", senderId, receiverId);
        } else {
            friendshipRepository.delete(friendship);
            log.info("Friendship request rejected between players {} and {}", senderId, receiverId);
        }

        if (response.getNotificationId() != null) {
            notificationRepository.deleteById(response.getNotificationId());
        } else {
            notificationService.deleteFriendRequestByPlayerIds(response.getSenderId(), response.getReceiverId());
        }
    }

    @Transactional
    public void removeAsFriend(PlayerInteraction removeFriendRequest) {
        log.info("Entering removeAsFriend: senderId = {}, receiverId = {}", removeFriendRequest.getSenderId(), removeFriendRequest.getReceiverId());

        Long senderId = removeFriendRequest.getSenderId();
        Long receiverId = removeFriendRequest.getReceiverId();

        Player sendingPlayer = playerService.getPlayerById(senderId);
        Player receivingPlayer = playerService.getPlayerById(receiverId);
        log.debug("Player {} is removing player {} from their friends", senderId, receiverId);

        Optional<Friendship> existingFriendship = friendshipRepository
                .findExistingFriendshipByPlayers(
                        sendingPlayer, receivingPlayer);

        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.ACTIVE) {
                friendshipRepository.delete(friendship);
                log.info("Removed friendship between players {} and {}", senderId, receiverId);
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                friendshipRepository.delete(friendship);
                notificationService.deleteFriendRequestByPlayerIds(receiverId, senderId);
                log.debug("Deleting pending friend request notification sent by player {} to player {}", receiverId, senderId);

            }
        }
    }

    public List<PlayerDTO> getFriends(Long playerId) {
        return playerMapper.multipleToDTO(friendshipRepository.getActiveFriends(playerId));
    }

    public FriendshipStatus getFriendshipStatus(RelationshipStatusRequest request) {
        Optional<Friendship> friendships = friendshipRepository
                .findFriendshipBetweenPlayerIds(request.getPlayerId(), request.getTargetPlayerId());
                        

        if (friendships.isEmpty()) {
            return FriendshipStatus.NONE;
        }

        Friendship friendship = friendships.get();

        if (friendship.getStatus() == FriendshipStatus.PENDING) {
            return friendship.getPlayer1().getId().equals(request.getPlayerId()) ?
                    FriendshipStatus.PENDING : FriendshipStatus.INCOMING_REQUEST;
        }
        return friendship.getStatus();
    }
}
