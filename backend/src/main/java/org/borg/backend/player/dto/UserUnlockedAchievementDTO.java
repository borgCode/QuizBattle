package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserUnlockedAchievementDTO {
    private String achievementName;
    private String achievementLevelName;
    private String description;
    private String imageUrl;
    private int nextLevelRequirement;
    private boolean isMaxLevel;
}
