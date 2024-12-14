package org.borg.backend.friendship;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.friendship.model.FriendshipStatus;
import org.borg.backend.friendship.model.PlayerInteraction;
import org.borg.backend.friendship.model.PlayerInteractionResponse;
import org.borg.backend.handler.BusinessErrorCodes;
import org.borg.backend.handler.FriendshipException;
import org.borg.backend.notification.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.player.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
            if (friendship.getPlayer1().equals(sendingPlayer)) {
                switch (friendship.getStatus()) {
                    case BLOCKED -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED);
                    case PENDING -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING);
                    case ACTIVE -> throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
                }
            } else {
                if (friendship.getStatus() == FriendshipStatus.PENDING) {
                    friendship.setStatus(FriendshipStatus.ACTIVE);
                    friendshipRepository.save(friendship);

                    //TODO Notification for accepted friend
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
        } else {
            friendshipRepository.delete(friendship);
        }
        
        notificationService.markAsRead(response.getNotificationId());
    }


    //TODO refactor code

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
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                friendship.setStatus(FriendshipStatus.BLOCKED);
                friendshipRepository.save(friendship);
                return;

            }
            if (friendship.getStatus() == FriendshipStatus.ACTIVE) {
                friendship.setStatus(FriendshipStatus.BLOCKED);
                friendshipRepository.save(friendship);
                return;
            }

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
