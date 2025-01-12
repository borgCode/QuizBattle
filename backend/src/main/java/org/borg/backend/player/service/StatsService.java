package org.borg.backend.player.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;

    @Transactional
    public void handleQuestionStats(Long playerId, String category, boolean isCorrect) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player not found for " + playerId));

        player.getStats().incrementQuestionsAnswered(category);

        if (isCorrect) {
            player.getStats().incrementCorrectAnswer(category);
        }

        playerRepository.save(player);
    }

    public void handleGameStats(Player player1, Player player2, GameResult result) {
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
