package org.borg.backend.friendship;

import lombok.RequiredArgsConstructor;
import org.borg.backend.handler.BusinessErrorCodes;
import org.borg.backend.handler.FriendshipException;
import org.borg.backend.player.Player;
import org.borg.backend.player.PlayerDTO;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.player.PlayerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final PlayerRepository playerRepository;

    public void sendFriendRequest(PlayerInteraction request) {
        Player sendingPlayer = playerRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Player receivingPlayer = playerRepository.findByUsername(request.getReceiverUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Optional<Friendship> existingFriendship = friendshipRepository.findByPlayer1AndPlayer2(sendingPlayer, receivingPlayer);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_BLOCKED);
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_REQUEST_PENDING);
            }
            if (friendship.getStatus() == FriendshipStatus.FRIENDS) {
                throw new FriendshipException(BusinessErrorCodes.FRIENDSHIP_ALREADY_EXISTS);
            }
        }

        friendshipRepository.save(new Friendship(
                sendingPlayer,
                receivingPlayer,
                LocalDate.now(),
                FriendshipStatus.PENDING
        ));
    }

    public void blockPlayer(PlayerInteraction request) {
        Player sendingPlayer = playerRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Player receivingPlayer = playerRepository.findByUsername(request.getReceiverUsername())
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
            if (friendship.getStatus() == FriendshipStatus.FRIENDS) {
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
        return PlayerMapper.multipleToDTO(friendshipRepository.getAllByPlayerId(playerId));
    }
}
