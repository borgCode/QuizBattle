package org.borg.backend.player.events;

import org.borg.backend.player.model.Player;
import org.borg.backend.game.shared.enums.GameResult;

public class StatsEvents {
    public record QuestionAnsweredEvent(Long playerId, String category, boolean isCorrect) {}
    public record GameCompletedEvent(Player player1, Player player2, GameResult result) {}
}
