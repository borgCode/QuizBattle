package org.borg.backend.social.friendship.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.dto.RelationshipStatusRequest;
import org.borg.backend.social.friendship.dto.RelationshipsDTO;
import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.social.friendship.model.FriendshipStatus;
import org.borg.backend.social.friendship.dto.PlayerInteraction;
import org.borg.backend.social.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.FriendshipException;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.social.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

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
        
        if (playerBlockService.checkBlocksForFriendRequest(senderId, receiverId)) {
            //TODO Archive notif for 14 days then delete
            return;
        }
        log.info("No existing blocks between Player {} and Player {}", senderId, receiverId);
        
        Player sendingPlayer = playerService.getPlayerById(request.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(request.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository.
                findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (existingFriendships.isEmpty()) {
            log.info("No existing friendship found, creating new friend request");
            friendshipRepository.save(Friendship.builder()
                    .player1(sendingPlayer)
                    .player2(receivingPlayer)
                    .friendShipDate(LocalDate.now())
                    .status(FriendshipStatus.PENDING)
                    .build());

            notificationService.sendFriendRequestNotification(receiverId, sendingPlayer);
        } else {
            Friendship friendship = existingFriendships.get(0);
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

                        //Clean up the friend request notif in the case of both sending a request
                        notificationService.deleteFriendRequestByPlayerIds(senderId, receiverId);
                }
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
        
        Player sendingPlayer = playerService.getPlayerById(response.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(response.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository.
                findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (existingFriendships.isEmpty()) {
            log.warn("No friendship found between players {} and {}, deleting notification", sendingPlayer.getId(), receivingPlayer.getId());
            notificationRepository.deleteById(response.getNotificationId());
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND,
                    String.format("No friendship found between players %d and %d",
                            sendingPlayer.getId(), receivingPlayer.getId()));
        }

        Friendship friendship = existingFriendships.get(0);

        if (friendship.getStatus().equals(FriendshipStatus.ACTIVE)) {
            log.warn("Friendship already exists between players {} and {}, cannot respond", sendingPlayer.getId(), receivingPlayer.getId());
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS,
                    String.format("Friendship already exists between players %d and %d",
                            sendingPlayer.getId(), receivingPlayer.getId()));
        }
        log.debug("Updating friendship status based on player response");

        if (wantsFriendship) {
            friendship.setStatus(FriendshipStatus.ACTIVE);
            friendshipRepository.save(friendship);
            notificationService.sendFriendAcceptedNotification(response.getReceiverId(), sendingPlayer);
            log.info("Friendship accepted between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
        } else {
            friendshipRepository.delete(friendship);
            log.info("Friendship request rejected between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
        }

        if (response.getNotificationId() != null) {
            notificationRepository.deleteById(response.getNotificationId());
        } else {
            notificationService.deleteFriendRequestByPlayerIds(response.getSenderId(), response.getReceiverId());
        }
    }

    @Transactional
    public void blockPlayer(PlayerInteraction blockRequest) {
        log.info("Entering blockPlayer: senderId = {}, receiverId = {}", blockRequest.getSenderId(), blockRequest.getReceiverId());
        
        Player sendingPlayer = playerService.getPlayerById(blockRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(blockRequest.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository
                .findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (!existingFriendships.isEmpty()) {
            Friendship existingFriendship = existingFriendships.get(0);
            log.debug("Existing friendship found with status: {}", existingFriendship.getStatus());
            
            if (existingFriendship.getStatus() == FriendshipStatus.BLOCKED &&
                    existingFriendship.getPlayer1().equals(sendingPlayer)) {
                log.warn("Player {} has already blocked player {}", sendingPlayer.getId(), receivingPlayer.getId());
                throw new FriendshipException(BusinessErrorCodes.ALREADY_BLOCKED_FRIENDSHIP,
                        String.format("Player %d has already blocked player %d",
                                sendingPlayer.getId(), receivingPlayer.getId()));
            }
            log.info("Deleted existing friendship between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
            friendshipRepository.delete(existingFriendship);
        }
        friendshipRepository.save(new Friendship(sendingPlayer, receivingPlayer, LocalDate.now(), FriendshipStatus.BLOCKED));
        log.info("Friendship between players {} and {} is now blocked", sendingPlayer.getId(), receivingPlayer.getId());
        
        notificationService.deleteFriendRequestByPlayerIds(sendingPlayer.getId(), receivingPlayer.getId());
        log.debug("Deleted pending friend request notification between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
    }

    public void unblockPlayer(PlayerInteraction unblockRequest) {
        log.info("Entering unblockPlayer: senderId = {}, receiverId = {}", unblockRequest.getSenderId(), unblockRequest.getReceiverId());

        Player sendingPlayer = playerService.getPlayerById(unblockRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(unblockRequest.getReceiverId());
        log.debug("Player {} is unblocking player {}", sendingPlayer.getId(), receivingPlayer.getId());

        Optional<Friendship> existingFriendship = friendshipRepository.findByPlayer1AndPlayer2(sendingPlayer, receivingPlayer);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.PENDING || friendship.getStatus() == FriendshipStatus.ACTIVE) {
                log.warn("Cannot unblock active or pending friendship between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
                throw new FriendshipException(BusinessErrorCodes.CANNOT_UNBLOCK_ACTIVE_FRIENDSHIP,
                        String.format("Cannot unblock active or pending friendship between players %d and %d",
                                sendingPlayer.getId(), receivingPlayer.getId()));
            }

            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                friendshipRepository.delete(friendship);
                log.info("Friendship between players {} and {} has been unblocked", sendingPlayer.getId(), receivingPlayer.getId());
            }
        } else {
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND,
                    String.format("No friendship found between players %d and %d",
                            sendingPlayer.getId(), receivingPlayer.getId()));
        }
    }

    @Transactional
    public void removeAsFriend(PlayerInteraction removeFriendRequest) {
        log.info("Entering removeAsFriend: senderId = {}, receiverId = {}", removeFriendRequest.getSenderId(), removeFriendRequest.getReceiverId());
        
        Player sendingPlayer = playerService.getPlayerById(removeFriendRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(removeFriendRequest.getReceiverId());
        log.debug("Player {} is removing player {} from their friends", sendingPlayer.getId(), receivingPlayer.getId());

        List<Friendship> existingFriendships = friendshipRepository
                .findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (!existingFriendships.isEmpty()) {
            Friendship friendship = existingFriendships.get(0);
            log.debug("Existing friendship found with status: {}", friendship.getStatus());
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                log.warn("Cannot remove blocked friendship between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());
                throw new FriendshipException(BusinessErrorCodes.ALREADY_BLOCKED_FRIENDSHIP,
                        String.format("Cannot remove blocked friendship between players %d and %d",
                                sendingPlayer.getId(), receivingPlayer.getId()));
            }
            if (friendship.getStatus() == FriendshipStatus.ACTIVE) {
                friendshipRepository.delete(friendship);
                log.info("Removed friendship between players {} and {}", sendingPlayer.getId(), receivingPlayer.getId());

            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                friendshipRepository.delete(friendship);
                notificationService.deleteFriendRequestByPlayerIds(receivingPlayer.getId(), sendingPlayer.getId());
                log.debug("Deleted pending friend request notification between players {} and {}", receivingPlayer.getId(), sendingPlayer.getId());
            }
        }
    }

    public List<PlayerDTO> getFriends(Long playerId) {
        return playerMapper.multipleToDTO(friendshipRepository.getActiveFriends(playerId));
    }

    public RelationshipsDTO getRelationships(Long playerId) {
        List<Player> friends = friendshipRepository.getActiveFriends(playerId);
        List<Player> blocked = friendshipRepository.getBlockedPlayers(playerId);

        return RelationshipsDTO.builder()
                .friends(playerMapper.multipleToDTO(friends))
                .blocked(playerMapper.multipleToDTO(blocked))
                .build();
    }

    public FriendshipStatus getRelationshipStatus(RelationshipStatusRequest request) {
        List<Friendship> friendships = friendshipRepository
                .findByPlayer1IdAndPlayer2IdOrPlayer1IdAndPlayer2Id(
                        request.getPlayerId(),
                        request.getTargetPlayerId(),
                        request.getTargetPlayerId(),
                        request.getPlayerId());

        if (friendships.isEmpty()) {
            return FriendshipStatus.NONE;
        }

        Friendship friendship = friendships.get(0);

        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            return friendship.getPlayer1().getId().equals(request.getPlayerId()) ?
                    FriendshipStatus.BLOCKED : FriendshipStatus.NONE;
        }

        if (friendship.getStatus() == FriendshipStatus.PENDING) {
            return friendship.getPlayer1().getId().equals(request.getPlayerId()) ?
                    FriendshipStatus.PENDING : FriendshipStatus.INCOMING_REQUEST;
        }
        return friendship.getStatus();
    }
}
