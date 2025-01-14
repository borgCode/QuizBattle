package org.borg.backend.social.block.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.dto.PlayerInteraction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("block")
@RequiredArgsConstructor
@Tag(name = "Block")
public class PlayerBlockController {

    private final PlayerBlockService playerBlockService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#blockerId)")
    @PostMapping("/block")
    public ResponseEntity<Void> blockPlayer(@RequestParam long blockerId, long blockedId) {
        playerBlockService.blockPlayer(blockerId, blockedId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#blockerId)")
    @PostMapping("/unblock")
    public ResponseEntity<Void> unblockPlayer(@RequestParam long blockerId, long blockedId) {
        playerBlockService.unblockPlayer(blockerId, blockedId);
        return ResponseEntity.ok().build();
    }
}
