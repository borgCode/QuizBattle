package org.borg.backend.achievement.events;

public class AchievementEvents {
    public record StoryCompletedEvent(Long playerId, String storyName) {}
    public record CategoryCompletedEvent(Long playerId, String category) {}
    public record GameWonEvent(Long playerId) {}
}
