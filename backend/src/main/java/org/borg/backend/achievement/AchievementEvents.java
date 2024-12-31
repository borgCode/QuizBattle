package org.borg.backend.achievement;

public class AchievementEvents {
    public record StoryCompletedEvent(Long playerId, Long storyId) {}
    public record CategoryCompletedEvent(Long playerId, String category, int totalCorrect) {}
    public record GameWonEvent(Long playerId) {}
}
