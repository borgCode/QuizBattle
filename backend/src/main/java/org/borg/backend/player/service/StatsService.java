package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    @Transactional
    public void updateQuestionStats(Long playerId, String category, boolean isCorrect) {
        log.debug("Starting stats update for {} in category {}", playerId, category);
        
        Player player = playerService.getPlayerById(playerId);

        player.getStats().incrementQuestionsAnswered(category);

        if (isCorrect) {
            player.getStats().incrementCorrectAnswer(category);
        }
        
        Player updatedPlayer = playerRepository.save(player);

        log.debug("After update, correct answers: {}", updatedPlayer.getStats().getCategoryStats().get(category).getCorrect());
    }

    @Transactional
    public void updateGameStats(Player player1, Player player2, GameResult result) {
        Stats player1Stats = player1.getStats();
        Stats player2Stats = player2.getStats();

        if (result.equals(GameResult.WIN_PLAYER1)) {
            player1Stats.incrementWins();
            player2Stats.incrementLosses();
        } else if (result.equals(GameResult.WIN_PLAYER2)) {
            player1Stats.incrementLosses();
            player2Stats.incrementWins();
        } else {
            player1Stats.incrementTies();
            player2Stats.incrementTies();
        }

        player1.setStats(player1Stats);
        player2.setStats(player2Stats);

        playerRepository.saveAll(List.of(player1, player2));
    }
}
