package org.borg.backend.social.block.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.BlockException;
import org.borg.backend.social.block.dto.BlockRequest;
import org.borg.backend.social.block.model.PlayerBlock;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerBlockService {

    private final PlayerService playerService;
    private final PlayerBlockRepository playerBlockRepository;

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
        
        
        
        playerBlockRepository.save(PlayerBlock.builder()
                .blocker(sendingPlayer)
                .blocked(receivingPlayer)
                .blockDate(Instant.now())
                .build());
    }
}
