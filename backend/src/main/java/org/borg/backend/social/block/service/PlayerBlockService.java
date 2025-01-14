package org.borg.backend.social.block.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.dto.BlockRequest;
import org.borg.backend.social.block.event.PlayerBlockEvent;
import org.borg.backend.social.block.model.PlayerBlock;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.borg.backend.social.block.event.PlayerBlockEvent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerBlockService {

    private final PlayerService playerService;
    private final PlayerBlockRepository playerBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void blockPlayer(BlockRequest blockRequest) {
        log.info("Entering blockPlayer: senderId = {}, receiverId = {}", blockRequest.getSenderId(), blockRequest.getReceiverId());

        Long senderId = blockRequest.getSenderId();
        Long receiverId = blockRequest.getReceiverId();

        if (senderId.equals(receiverId)) {
            log.warn("Player {} attempted to block themselves", senderId);
            throw new BlockException(BusinessErrorCodes.CANNOT_BLOCK_SELF, String.format("Player %d attempted to block themselves", senderId));
        }

        if (playerBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)) {
            log.warn("Player {} has already blocked player {}", senderId, receiverId);
            throw new BlockException(BusinessErrorCodes.ALREADY_BLOCKED, String.format("Player %d has already blocked player %d",
                    senderId, receiverId));
        }

        Player sendingPlayer = playerService.getPlayerById(blockRequest.getSenderId());
        Player receivingPlayer = playerService.getPlayerById(blockRequest.getReceiverId());
        
        PlayerBlock block = playerBlockRepository.save(PlayerBlock.builder()
                .blocker(sendingPlayer)
                .blocked(receivingPlayer)
                .blockDate(Instant.now())
                .build());
        log.info("Block created between players {} and {}", senderId, receiverId);
        
        applicationEventPublisher.publishEvent(new PlayerBlockedEvent(block));
        log.info("Published player blocked event for {} and {}", senderId, receiverId);
    }
}
