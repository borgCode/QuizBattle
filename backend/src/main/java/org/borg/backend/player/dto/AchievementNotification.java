package org.borg.backend.player.dto;

import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class AchievementNotification {
    private final String achievementName;
    private final String achievementDescription;
    private String base64Image;
    private final Instant earnedAt;
}
