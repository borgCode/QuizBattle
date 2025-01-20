package org.borg.backend.unit.game.multiplayer.service;

import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.service.GameService;
import org.borg.backend.game.multiplayer.service.MultiplayerSessionService;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.social.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameServiceTest {
    
    @Mock
    private NotificationService notificationService;
    @Mock
    private MultiplayerSessionService multiplayerSessionService;
    @Mock
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private GameService gameService;
    

    @BeforeEach
    void setUp() {
        
        MockitoAnnotations.openMocks(this);
    }
    
    @Nested
    class UpdateGameState_ShouldCompleteGameTests {
        
        @Test
        void shouldHandlePlayer1Win() {
            MultiplayerSession session = createTestSession(15, 3, 17, 18);
            
            when(multiplayerSessionService.getSessionById(session.getId())).thenReturn(session);

            gameService.updateGameState(1L, 1L, 1L, true);

            assertAll("Post game completion checks",
                    () -> assertEquals(GameStatus.COMPLETED, session.getStatus(), "Session should be marked as complete"),
                    () -> assertEquals(1L, session.getWinnerId(), "Player 1 should be marked as the winner"),
                    () -> assertEquals(2L, session.getLoserId(), "Player 2 should be marked as the loser"),
                    () -> verify(statsService).updateGameStats(any(), any(), eq(GameResult.WIN_PLAYER1)),
                    () -> verify(eventPublisher).publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId())),
                    () -> verify(multiplayerSessionRepository).save(session),
                    () -> verify(notificationService).sendGameWonNotification(1L, "player2", session.getId()),
                    () -> verify(notificationService).sendGameLostNotification(2L, "player1", session.getId()),
                    () -> verify(simpMessagingTemplate).convertAndSendToUser(
                            "player2",
                            "queue/session/" + session.getId(),
                            "refresh"
                    ));
            
        }

        @Test
        void shouldHandlePlayer2Win() {
            MultiplayerSession session = createTestSession(3, 15, 17, 18);
            when(multiplayerSessionService.getSessionById(session.getId())).thenReturn(session);

            gameService.updateGameState(1L, 1L, 1L, true);

            assertAll("Post game completion checks",
                    () -> assertEquals(GameStatus.COMPLETED, session.getStatus(), "Session should be marked as complete"),
                    () -> assertEquals(2L, session.getWinnerId(), "Player 2 should be marked as the winner"),
                    () -> assertEquals(1L, session.getLoserId(), "Player 1 should be marked as the loser"),
                    () -> verify(statsService).updateGameStats(any(), any(), eq(GameResult.WIN_PLAYER2)),
                    () -> verify(eventPublisher).publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId())),
                    () -> verify(multiplayerSessionRepository).save(session),
                    () -> verify(notificationService).sendGameWonNotification(2L, "player1", session.getId()),
                    () -> verify(notificationService).sendGameLostNotification(1L, "player2", session.getId()),
                    () -> verify(simpMessagingTemplate).convertAndSendToUser(
                            "player2",
                            "queue/session/" + session.getId(),
                            "refresh"
                    ));
        }

        @Test
        void shouldHandleTie() {
            MultiplayerSession session = createTestSession(11, 12, 17, 18);
            when(multiplayerSessionService.getSessionById(session.getId())).thenReturn(session);

            gameService.updateGameState(1L, 1L, 1L, true);

            assertAll("Post game completion checks",
                    () -> assertEquals(GameStatus.COMPLETED, session.getStatus(), "Session should be marked as complete"),
                    () -> assertNull(session.getWinnerId(), "Winner should be null in tie"),
                    () -> assertNull(session.getLoserId(), "Loser should be null in tie"),
                    () -> assertTrue(session.getIsTie(), "Game should be marked as tie"),
                    () -> verify(statsService).updateGameStats(any(), any(), eq(GameResult.TIE)),
                    () -> verify(multiplayerSessionRepository).save(session),
                    () -> verify(notificationService).sendTieNotifications(session.getSessionPlayers(), session.getId()),
                    () -> verify(simpMessagingTemplate).convertAndSendToUser(
                            "player2",
                            "queue/session/" + session.getId(),
                            "refresh"
                    ));
        }
    }

    @Test
    void shouldUpdateStateWithoutCompletingGame() {
        MultiplayerSession session = createTestSession(5, 3, 8, 8);
        when(multiplayerSessionService.getSessionById(session.getId())).thenReturn(session);

        gameService.updateGameState(1L, 1L, 1L, true);

        assertAll("Mid-game state checks",
                () -> assertEquals(GameStatus.ACTIVE, session.getStatus()),
                () -> assertEquals(6, session.getSessionPlayers().get(0).getScore()),
                () -> assertEquals(9, session.getSessionPlayers().get(0).getQuestionsAnswered()),
                () -> verify(multiplayerSessionRepository).save(session));
    }
    
    @Test
    void giveUpTest() {
        MultiplayerSession session = createTestSession(9, 12, 9, 12);
        when(multiplayerSessionService.getSessionById(session.getId())).thenReturn(session);
        
        gameService.handleGiveUp(session.getId(), 1L);

        assertAll("Post give up checks",
                () -> assertEquals(GameStatus.COMPLETED, session.getStatus(), "Session should be marked as complete"),
                () -> assertEquals(2L, session.getWinnerId(), "Player2 should be the winner"),
                () -> assertEquals(1L, session.getLoserId(), "Player1 should be the loser"),
                () -> assertTrue(session.getSessionPlayers().get(0).isGivenUp(), "Player 1 should be marked as given up"),
                () -> verify(statsService).updateGameStats(any(), any(), eq(GameResult.WIN_PLAYER2)),
                () -> verify(multiplayerSessionRepository).save(session),
                () -> verify(notificationService).sendGameWonNotification(2L, "player1", session.getId()),
                () -> verify(notificationService).sendGameLostNotification(1L, "player2", session.getId()));
    }
    
    @Test
    void updateQuestionsAndCategoryTest(){
        MultiplayerSession multiplayerSession = createTestSession(3, 3, 3, 3);
        multiplayerSession.getRoundCategories().add("History");
        multiplayerSession.getPlayedCategories().add("History");
        multiplayerSession.setQuestionIds(new ArrayList<>(List.of(1L, 2L, 3L)));

        List<Long> newQuestionIds = List.of(4L, 5L, 6L);
        String newCategory = "Geography";
        
        gameService.updateSessionQuestionsAndCategory(multiplayerSession,newQuestionIds, newCategory);

        assertEquals(newQuestionIds, multiplayerSession.getQuestionIds());
        assertTrue(multiplayerSession.getPlayedCategories().contains(newCategory));
        assertTrue(multiplayerSession.getRoundCategories().contains(newCategory));
    }

    private MultiplayerSession createTestSession(int player1Score, int player2Score, int player1QuestionsAnswered, int player2QuestionsAnswered) {
        Player player1 = createTestPlayer(1L, "player1");
        Player player2 = createTestPlayer(2L, "player2");

        MultiplayerSession session = new MultiplayerSession();
        session.setId(1L);
        session.setCurrentQuestionIndex(3);

        SessionPlayer sessionPlayer1 = new SessionPlayer();
        sessionPlayer1.setId(1L);
        sessionPlayer1.setPlayer(player1);
        sessionPlayer1.setScore(player1Score);
        sessionPlayer1.setQuestionsAnswered(player1QuestionsAnswered);

        SessionPlayer sessionPlayer2 = new SessionPlayer();
        sessionPlayer2.setId(2L);
        sessionPlayer2.setPlayer(player2);
        sessionPlayer2.setScore(player2Score);
        sessionPlayer2.setQuestionsAnswered(player2QuestionsAnswered);

        session.setSessionPlayers(Arrays.asList(sessionPlayer1, sessionPlayer2));
        session.setStatus(GameStatus.ACTIVE);
        session.setCurrentPlayerTurnId(player1.getId());

        return session;
    }

    static Player createTestPlayer(Long id, String name) {
        return Player.builder()
                .id(id)
                .username(name)
                .displayName(name)
                .build();
    }
}
