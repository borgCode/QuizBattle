package org.borg.backend.social.block.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.model.PlayerBlock;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.borg.backend.social.block.event.PlayerBlockEvent.PlayerBlockedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerBlockService {

    private final PlayerService playerService;
    private final PlayerBlockRepository playerBlockRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

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
}
