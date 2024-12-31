package org.borg.backend.achievement.events;

public class AchievementEvents {
    public record StoryCompletedEvent(Long playerId, String storyName) {}
    public record CategoryCompletedEvent(Long playerId, String category, int totalCorrect) {}
    public record GameWonEvent(Long playerId) {}
}
