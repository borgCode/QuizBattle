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
        log.debug("Starting stats update for playerId={}, category={}", playerId, category);

        Player player = playerService.getPlayerById(playerId);
        Stats stats = player.getStats();
        
        stats.incrementQuestionsAnswered(category);

        if (isCorrect) {
            stats.incrementCorrectAnswer(category);
            log.debug("Correct answer incremented for playerId={}, category={}", playerId, category);
        }

        playerRepository.save(player);
        log.info("Updated stats for playerId={}, category={}: correct={}, total={}",
                playerId, category, stats.getCategoryStats().get(category).getCorrect(),
                stats.getCategoryStats().get(category).getQuestionsAnswered());
    }

    @Transactional
    public void updateGameStats(Player player1, Player player2, GameResult result) {
        log.debug("Updating game stats for players {} and {} with result {}",
                player1.getId(), player2.getId(), result);
        
        Stats player1Stats = player1.getStats();
        Stats player2Stats = player2.getStats();

        log.debug("Current stats - Player {}: W={}/L={}/T={}, Player {}: W={}/L={}/T={}",
                player1.getId(),
                player1Stats.getNumOfWins(), player1Stats.getNumOfLosses(), player1Stats.getNumOfTies(),
                player2.getId(),
                player2Stats.getNumOfWins(), player2Stats.getNumOfLosses(), player2Stats.getNumOfTies());

        if (result.equals(GameResult.WIN_PLAYER1)) {
            player1Stats.incrementWins();
            player2Stats.incrementLosses();
            log.info("Game result: Player {} won against Player {}", player1.getId(), player2.getId());
        } else if (result.equals(GameResult.WIN_PLAYER2)) {
            player1Stats.incrementLosses();
            player2Stats.incrementWins();
            log.info("Game result: Player {} won against Player {}", player2.getId(), player1.getId());
        } else {
            player1Stats.incrementTies();
            player2Stats.incrementTies();
            log.info("Game result: Tie between Player {} and Player {}", player1.getId(), player2.getId());
        }

        player1.setStats(player1Stats);
        player2.setStats(player2Stats);

        playerRepository.saveAll(List.of(player1, player2));
        log.debug("Updated stats - Player {}: W={}/L={}/T={}, Player {}: W={}/L={}/T={}",
                player1.getId(),
                player1Stats.getNumOfWins(), player1Stats.getNumOfLosses(), player1Stats.getNumOfTies(),
                player2.getId(),
                player2Stats.getNumOfWins(), player2Stats.getNumOfLosses(), player2Stats.getNumOfTies());
    }
}
