package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
public class AchievementLevelDTO {
    private String name;
    private String description;
    private String imageUrl;
    private Instant achievedAt;
}
