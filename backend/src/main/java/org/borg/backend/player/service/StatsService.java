package org.borg.backend.player.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;

    @Transactional
    public void handleQuestionStats(Long playerId, String category, boolean isCorrect) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        player.getStats().incrementQuestionsAnswered(category);

        if (isCorrect) {
            player.getStats().incrementCorrectAnswer(category);
        }

        playerRepository.save(player);
    }
}
