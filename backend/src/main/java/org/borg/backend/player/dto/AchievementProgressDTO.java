package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AchievementProgressDTO {
    private int currentProgress;
    private int requirementForNext;
    private double progressPercentage;

}
