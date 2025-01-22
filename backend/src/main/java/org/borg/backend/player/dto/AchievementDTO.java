package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class AchievementDTO {
    private String name;
    private int totalLevels;
    private List<AchievementLevelDTO> unlockedLevels;
    private AchievementLevelDTO nextLevel;
    private AchievementProgressDTO progress;
}
