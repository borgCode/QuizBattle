package org.borg.backend.unit.player;

import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.player.model.CategoryStats;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.player.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            
            when(playerService.getPlayerById(player.getId())).thenReturn(player);
            
            statsService.updateQuestionStats(player.getId(), category, true);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            verify(playerRepository).save(playerCaptor.capture());

            Player capturedPlayer = playerCaptor.getValue();
            Map<String, CategoryStats> stats = capturedPlayer.getStats().getCategoryStats();

            assertAll("New category initialization check",
                    () -> assertEquals(1, stats.size(), "Should only have one category"),
                    () -> assertEquals(1, stats.get(category).getCorrect(), "Should have one correct answer"),
                    () -> assertEquals(1, stats.get(category).getQuestionsAnswered(), "Should have one question answered")
            );
        }
        
        @Test
        void shouldOnlyIncrementTotalWhenAnswerIsIncorrect() {
            String category = "History";

            when(playerService.getPlayerById(player.getId())).thenReturn(player);

            statsService.updateQuestionStats(player.getId(), category, false);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            verify(playerRepository).save(playerCaptor.capture());

            Player capturedPlayer = playerCaptor.getValue();
            assertEquals(0, capturedPlayer.getStats().getCategoryStats().get(category).getCorrect());
            assertEquals(1, capturedPlayer.getStats().getCategoryStats().get(category).getQuestionsAnswered());
        }
    }
    
    @Nested
    class UpdateGameStatsTests {
        Player player2;
        
        @BeforeEach
        void setUp() {
            player2 = Player.builder()
                    .id(2L)
                    .username("player2")
                    .stats(new Stats())
                    .build();
        }
        
        @Test
        void shouldIncrementWinsAndLossesWhenPlayer1Wins() {
            statsService.updateGameStats(player, player2, GameResult.WIN_PLAYER1);

            @SuppressWarnings("unchecked") 
            ArgumentCaptor<List<Player>> playerCaptor = ArgumentCaptor.forClass(List.class);
            verify(playerRepository).saveAll(playerCaptor.capture());

            List<Player> capturedPlayers = playerCaptor.getValue();
            assertEquals(2, capturedPlayers.size());
            
            assertAll("Post stats update check",
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfGames(), "Player should have 1 game played"),
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfWins(), "Player should have 1 win"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfLosses(), "Player should have 0 losses"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfTies(), "Player 1 should have 0 ties"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfGames(), "Player 2 should have 1 game played"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfWins(), "Player 2 should have 0 wins"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfLosses(), "Player 2 should have 1 loss"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfTies(), "Player 2 should have 0 ties")
            );
        }

        @Test
        void shouldIncrementWinsAndLossesWhenPlayer2Wins() {
            statsService.updateGameStats(player, player2, GameResult.WIN_PLAYER2);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Player>> playerCaptor = ArgumentCaptor.forClass(List.class);
            verify(playerRepository).saveAll(playerCaptor.capture());

            List<Player> capturedPlayers = playerCaptor.getValue();
            assertEquals(2, capturedPlayers.size());

            assertAll("Post stats update check",
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfGames(), "Player 1 should have 1 game played"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfWins(), "Player 1 should have 0 wins"),
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfLosses(), "Player 1 should have 1 loss"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfTies(), "Player 1 should have 0 ties"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfGames(), "Player 2 should have 1 game played"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfWins(), "Player 2 should have 1 win"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfLosses(), "Player 2 should have 0 losses"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfTies(), "Player 2 should have 0 ties")
            );
        }

        @Test
        void shouldIncrementTiesForBothPlayersWhenGameIsTied() {
            statsService.updateGameStats(player, player2, GameResult.TIE);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Player>> playerCaptor = ArgumentCaptor.forClass(List.class);
            verify(playerRepository).saveAll(playerCaptor.capture());

            List<Player> capturedPlayers = playerCaptor.getValue();
            assertEquals(2, capturedPlayers.size());

            assertAll("Post tie game stats check",
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfGames(), "Player 1 should have 1 game played"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfWins(), "Player 1 should have 0 wins"),
                    () -> assertEquals(0, capturedPlayers.get(0).getStats().getNumOfLosses(), "Player 1 should have 0 losses"),
                    () -> assertEquals(1, capturedPlayers.get(0).getStats().getNumOfTies(), "Player 1 should have 1 tie"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfGames(), "Player 2 should have 1 game played"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfWins(), "Player 2 should have 0 wins"),
                    () -> assertEquals(0, capturedPlayers.get(1).getStats().getNumOfLosses(), "Player 2 should have 0 losses"),
                    () -> assertEquals(1, capturedPlayers.get(1).getStats().getNumOfTies(), "Player 2 should have 1 tie")
            );
        }
    }
}
