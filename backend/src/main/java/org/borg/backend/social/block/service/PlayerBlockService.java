package org.borg.backend.social.block.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.event.PlayerBlockEvent;
import org.borg.backend.social.block.model.PlayerBlock;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.borg.backend.social.friendship.dto.RelationshipStatusRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.borg.backend.social.block.event.PlayerBlockEvent.PlayerBlockedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerBlockService {

    private final PlayerService playerService;
    private final PlayerBlockRepository playerBlockRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlayerMapper playerMapper;

    @Transactional
    public void blockPlayer(Long blockerId, Long blockedId) {
        log.info("Entering blockPlayer: blockerId = {}, blockedId = {}", blockerId, blockedId);

        if (blockerId.equals(blockedId)) {
            log.warn("Player {} attempted to block themselves", blockerId);
            throw new BlockException(BusinessErrorCodes.CANNOT_BLOCK_SELF, String.format("Player %d attempted to block themselves", blockerId));
        }

        if (playerBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            log.warn("Player {} has already blocked player {}", blockerId, blockedId);
            throw new BlockException(BusinessErrorCodes.ALREADY_BLOCKED, String.format("Player %d has already blocked player %d",
                    blockerId, blockedId));
        }

        Player blockingPlayer = playerService.getPlayerById(blockerId);
        Player blockedPlayer = playerService.getPlayerById(blockedId);

        PlayerBlock block = playerBlockRepository.save(PlayerBlock.builder()
                .blocker(blockingPlayer)
                .blocked(blockedPlayer)
                .blockDate(Instant.now())
                .build());
        log.info("Block created between players {} and {}", blockerId, blockedId);

        applicationEventPublisher.publishEvent(new PlayerBlockedEvent(block));
        log.info("Published player blocked event for {} and {}", blockerId, blockedId);
    }

    @Transactional
    public void unblockPlayer(Long blockerId, Long blockedId) {
        log.info("Entering unblockPlayer: blockerId = {}, blockedId = {}", blockerId, blockedId);

        if (blockerId.equals(blockedId)) {
            log.warn("Player {} attempted to unblock themselves", blockerId);
            throw new BlockException(BusinessErrorCodes.CANNOT_UNBLOCK_SELF, String.format("Player %d attempted to unblock themselves", blockerId));
        }

        if (!playerBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            log.warn("Player {} tried to unblock {} with no block active", blockerId, blockedId);
            throw new BlockException(BusinessErrorCodes.CANNOT_UNBLOCK_WHEN_NOT_BLOCKED, String.format("Player %d tried to unblock %d with no block active",
                    blockerId, blockedId));
        }

        playerBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        log.info("Block from {} to {} removed", blockerId, blockedId);

        if (playerBlockRepository.existsByBlockerIdAndBlockedId(blockedId, blockerId)) {
            log.info("Block still exists from {} to {}", blockedId, blockerId);
        } else {
            applicationEventPublisher.publishEvent(new PlayerBlockEvent.PlayedUnblockedEvent(blockerId, blockedId));
            log.info("Published player unblocked event for {} and {}", blockerId, blockedId);
        }
    }

    public boolean checkBlocksForFriendRequest(Long senderId, Long receiverId) {
        if (playerBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)) {
            log.warn("Player {} tried to send a friend request to blocked player {}", senderId, receiverId);
            throw new BlockException(BusinessErrorCodes.CANNOT_SEND_FRIEND_REQUEST_TO_BLOCKED,
                    String.format("Player %d tried to send a friend request to blocked player %d", senderId, receiverId));
        }
        boolean isBlocked = playerBlockRepository.existsByBlockerIdAndBlockedId(receiverId, senderId);
        if (isBlocked) {
            log.info("Player {} has blocked sender {} - silently preventing interaction", receiverId, senderId);
        }
        return isBlocked;
    }

    public List<PlayerDTO> getBlocked(long playerId) {
        return playerMapper.multipleToDTO(playerBlockRepository.getBlockedPlayers(playerId));
    }

    public boolean checkIfBlockIsActive(Long blockerId, Long blockedId) {
        return playerBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean checkIfAnyBlockExists(Long player1, Long player2) {
        return playerBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(player1, player2, player2, player1);
    }
}
