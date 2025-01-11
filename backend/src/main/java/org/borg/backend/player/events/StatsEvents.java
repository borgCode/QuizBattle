package org.borg.backend.player.events;

public class StatsEvents {
    public record QuestionAnsweredEvent(Long playerId, String category, boolean isCorrect) {}
}
