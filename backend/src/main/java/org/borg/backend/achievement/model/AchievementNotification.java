package org.borg.backend.achievement.model;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class AchievementNotification {
    private final String achievementName;
    private final String achievementDescription;
    private final LocalDateTime earnedAt;
}
