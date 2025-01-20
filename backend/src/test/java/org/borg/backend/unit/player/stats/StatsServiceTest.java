package org.borg.backend.unit.player.stats;

import org.aspectj.lang.annotation.Before;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.player.service.StatsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatsServiceTest {
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerService playerService;
    
    @InjectMocks
    private StatsService statsService;
    
    private Player player;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player = Player.builder()
                .id(1L)
                .username("player1")
                .stats(new Stats())
                .build();
    }
    
    @Nested
    class UpdateQuestionStats {
        
        @Test
        void shouldIncrementCorrectAnswerAndTotalWhenAnswerIsCorrect() {
            String category = "History";
            boolean isCorrect = true;
            
            when(playerService.getPlayerById(player.getId())).thenReturn(player);
            
            statsService.updateQuestionStats(player.getId(), category, isCorrect);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            verify(playerRepository).save(playerCaptor.capture());

            Player capturedPlayer = playerCaptor.getValue();
            Assertions.assertEquals(1, capturedPlayer.getStats().getCategoryStats().get(category).getCorrect());
            Assertions.assertEquals(1, capturedPlayer.getStats().getCategoryStats().get(category).getQuestionsAnswered());
        }
    }
}
