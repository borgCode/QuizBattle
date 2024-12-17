package org.borg.backend.friendship.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.common.enums.FriendshipStatus;
import org.borg.backend.friendship.dto.PlayerInteraction;
import org.borg.backend.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.exceptions.FriendshipException;
import org.borg.backend.friendship.repository.FriendshipRepository;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final PlayerRepository playerRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendFriendRequest(PlayerInteraction request) {
        Player sendingPlayer = playerRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Player receivingPlayer = playerRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

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

            //Send notification to receiving player

            notificationService.sendFriendRequestNotification(receivingPlayer.getId(), sendingPlayer);
        } else {
            Friendship friendship = existingFriendships.get(0);
            log.warn("Friendship status: " + friendship.getStatus());
            if (friendship.getPlayer1().equals(sendingPlayer)) {
                switch (friendship.getStatus()) {
                    case BLOCKED -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED);
                    case PENDING -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING);
                    case ACTIVE -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
                }
            } else if (friendship.getPlayer2().equals(sendingPlayer)) {
                switch (friendship.getStatus()) {
                    case ACTIVE -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
                    case BLOCKED -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED);
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
        Player sendingPlayer = playerRepository.findById(response.getSenderId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Player receivingPlayer = playerRepository.findById(response.getReceiverId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Friendship> existingFriendships = friendshipRepository.
                findByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                        sendingPlayer, receivingPlayer, receivingPlayer, sendingPlayer);

        if (existingFriendships.isEmpty()) {
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
            notificationService.markAsRead(response.getNotificationId());
        }
    }

    
    public void blockPlayer(PlayerInteraction request) {
        Player sendingPlayer = playerRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Player receivingPlayer = playerRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Optional<Friendship> existingFriendship = friendshipRepository.findByPlayer1AndPlayer2(sendingPlayer, receivingPlayer);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED);
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING || friendship.getStatus() == FriendshipStatus.ACTIVE) {
                friendship.setStatus(FriendshipStatus.BLOCKED);
                friendshipRepository.save(friendship);
            }
        } else {
            friendshipRepository.save(new Friendship(
                    sendingPlayer,
                    receivingPlayer,
                    LocalDate.now(),
                    FriendshipStatus.BLOCKED
            ));
        }
    }

    public List<PlayerDTO> getFriends(Long playerId) {
        return PlayerMapper.multipleToDTO(friendshipRepository.getAllByPlayerId(playerId, FriendshipStatus.ACTIVE));
    }
}
