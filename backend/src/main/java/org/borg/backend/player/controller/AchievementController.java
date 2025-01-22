package org.borg.backend.player.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.player.dto.AchievementDTO;
import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.service.AchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("achievement")
@RequiredArgsConstructor
@Tag(name = "Achievement")
public class AchievementController {

    private final AchievementService achievementService;


    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/{playerId}")
    public ResponseEntity<List<AchievementDTO>> getUnlockedAchievements(@PathVariable long playerId) {
        return ResponseEntity.ok(achievementService.getUnlockedAchievements(playerId));
    }
}
