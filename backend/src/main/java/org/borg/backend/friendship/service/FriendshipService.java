package org.borg.backend.friendship.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.friendship.dto.RelationshipStatusRequest;
import org.borg.backend.friendship.dto.RelationshipsDTO;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.friendship.model.FriendshipStatus;
import org.borg.backend.friendship.dto.PlayerInteraction;
import org.borg.backend.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.FriendshipException;
import org.borg.backend.friendship.repository.FriendshipRepository;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.notification.service.NotificationService;
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

    @Transactional
    public void sendFriendRequest(PlayerInteraction request) {
        Player sendingPlayer = playerService.getPlayerById(request.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(request.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository.
                findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (existingFriendships.isEmpty()) {

            friendshipRepository.save(Friendship.builder()
                    .player1(sendingPlayer)
                    .player2(receivingPlayer)
                    .friendShipDate(LocalDate.now())
                    .status(FriendshipStatus.PENDING)
                    .build());

            notificationService.sendFriendRequestNotification(receivingPlayer.getId(), sendingPlayer);
        } else {
            Friendship friendship = existingFriendships.get(0);
            if (friendship.getPlayer1().equals(sendingPlayer)) {
                switch (friendship.getStatus()) {
                    case BLOCKED ->
                            throw new FriendshipException(BusinessErrorCodes.CANNOT_SENT_REQUEST_TO_BLOCKED_PLAYER);
                    case PENDING -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING);
                    case ACTIVE -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
                }
            } else if (friendship.getPlayer2().equals(sendingPlayer)) {
                switch (friendship.getStatus()) {
                    case ACTIVE -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
                    case BLOCKED ->
                            throw new FriendshipException(BusinessErrorCodes.CANNOT_SENT_REQUEST_TO_BLOCKED_PLAYER);
                    case PENDING -> {
                        friendship.setStatus(FriendshipStatus.ACTIVE);
                        friendshipRepository.save(friendship);
                        notificationService.sendFriendAcceptedNotification(friendship.getPlayer1().getId(), sendingPlayer);
                        notificationService.sendFriendAcceptedNotification(friendship.getPlayer2().getId(), receivingPlayer);

                        //Clean up the friend request notif in the case of both sending a request
                        notificationService.deleteFriendRequestByPlayerIds(sendingPlayer.getId(), receivingPlayer.getId());
                    }
                }
            }
        }
    }

    @Transactional
    public void handleFriendshipResponse(PlayerInteractionResponse response, boolean wantsFriendship) {
        Player sendingPlayer = playerService.getPlayerById(response.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(response.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository.
                findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (existingFriendships.isEmpty()) {
            notificationRepository.deleteById(response.getNotificationId());
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND);
        }

        Friendship friendship = existingFriendships.get(0);

        if (friendship.getStatus().equals(FriendshipStatus.ACTIVE)) {
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
        }

        if (wantsFriendship) {
            friendship.setStatus(FriendshipStatus.ACTIVE);
            friendshipRepository.save(friendship);
            notificationService.sendFriendAcceptedNotification(response.getReceiverId(), sendingPlayer);
        } else {
            friendshipRepository.delete(friendship);
        }

        if (response.getNotificationId() != null) {
            notificationRepository.deleteById(response.getNotificationId());
        } else {
            notificationService.deleteFriendRequestByPlayerIds(response.getSenderId(), response.getReceiverId());
        }
    }

    @Transactional
    public void blockPlayer(PlayerInteraction blockRequest) {
        Player sendingPlayer = playerService.getPlayerById(blockRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(blockRequest.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository
                .findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (!existingFriendships.isEmpty()) {
            Friendship existingFriendship = existingFriendships.get(0);
            if (existingFriendship.getStatus() == FriendshipStatus.BLOCKED &&
                    existingFriendship.getPlayer1().equals(sendingPlayer)) {
                throw new FriendshipException(BusinessErrorCodes.ALREADY_BLOCKED_FRIENDSHIP);
            }
            friendshipRepository.delete(existingFriendship);
        }
        friendshipRepository.save(new Friendship(
                sendingPlayer,
                receivingPlayer,
                LocalDate.now(),
                FriendshipStatus.BLOCKED
        ));
        notificationService.deleteFriendRequestByPlayerIds(sendingPlayer.getId(), receivingPlayer.getId());
    }

    public void unblockPlayer(PlayerInteraction unblockRequest) {
        Player sendingPlayer = playerService.getPlayerById(unblockRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(unblockRequest.getReceiverId());

        Optional<Friendship> existingFriendship = friendshipRepository.findByPlayer1AndPlayer2(sendingPlayer, receivingPlayer);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.PENDING || friendship.getStatus() == FriendshipStatus.ACTIVE) {
                throw new FriendshipException(BusinessErrorCodes.CANNOT_UNBLOCK_ACTIVE_FRIENDSHIP);
            }

            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                friendshipRepository.delete(friendship);
            }
        } else {
            throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_NOT_FOUND);
        }
    }

    @Transactional
    public void removeAsFriend(PlayerInteraction removeFriendRequest) {
        Player sendingPlayer = playerService.getPlayerById(removeFriendRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(removeFriendRequest.getReceiverId());

        List<Friendship> existingFriendships = friendshipRepository
                .findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (!existingFriendships.isEmpty()) {
            Friendship friendship = existingFriendships.get(0);
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new FriendshipException(BusinessErrorCodes.ALREADY_BLOCKED_FRIENDSHIP);
            }
            if (friendship.getStatus() == FriendshipStatus.ACTIVE) {
                friendshipRepository.delete(friendship);
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                friendshipRepository.delete(friendship);
                notificationService.deleteFriendRequestByPlayerIds(receivingPlayer.getId(), sendingPlayer.getId());
            }
        }
    }

    public List<PlayerDTO> getFriends(Long playerId) {
        return PlayerMapper.multipleToDTO(friendshipRepository.getActiveFriends(playerId));
    }

    public RelationshipsDTO getRelationships(Long playerId) {
        List<Player> friends = friendshipRepository.getActiveFriends(playerId);
        List<Player> blocked = friendshipRepository.getBlockedPlayers(playerId);

        return RelationshipsDTO.builder()
                .friends(PlayerMapper.multipleToDTO(friends))
                .blocked(PlayerMapper.multipleToDTO(blocked))
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
