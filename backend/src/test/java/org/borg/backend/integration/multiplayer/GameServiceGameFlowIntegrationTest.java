package org.borg.backend.integration.multiplayer;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.dto.SessionPlayerDTO;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.service.GameService;
import org.borg.backend.game.multiplayer.service.MultiplayerQuestionService;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.model.RoundType;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.GameValidationService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.AchievementService;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GameServiceGameFlowIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Autowired
    private GameService gameService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private RoundSessionService roundSessionService;
    @Autowired
    private MultiplayerQuestionService multiplayerQuestionService;
    @Autowired
    private GameValidationService gameValidationService;
    
    @MockitoBean
    private AchievementService achievementService;
    @MockitoBean
    private StatsService statsService;
    
    private Player player1;
    private Player player2;
    

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        multiplayerSessionRepository.deleteAll();
        playerRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        player1 = createAndSavePlayer("player1");
        player2 = createAndSavePlayer("player2");

    }

    private Player createAndSavePlayer(String name) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username(name.toLowerCase())
                .password("password")
                .displayName(name)
                .accountLocked(false)
                .enabled(true)
                .roles(new ArrayList<>(List.of(userRole)))
                .build();

        return playerRepository.save(player);
    }

    @Test
    void testAnswerValidationAndGameStateUpdate() {

        MultiplayerSession multiplayerSession = new MultiplayerSession(player1, player2, player1.getId());
        multiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(multiplayerSession);

        List<Question> questions = loadQuestionsToDB();

        List<Long> expectedQuestionIds = questions.stream()
                .map(Question::getId)
                .toList();

        roundSessionService.initializeSession(player1.getId(), expectedQuestionIds, "Sports", RoundType.MULTIPLAYER);
        gameService.updateSessionQuestionsAndCategory(multiplayerSession, expectedQuestionIds, "Sports");

        questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

        GameStateResponse gameStateResponse = gameService.getGameState(multiplayerSession.getId(), player1.getId());


        Long actualPlayerTurn = gameStateResponse.getPlayerTurn();
        SessionPlayerDTO actualPlayerDTO = gameStateResponse.getPlayerDTO();
        SessionPlayerDTO actualOpponentDTO = gameStateResponse.getOpponentDTO();
        List<Long> actualQuestionIds = gameStateResponse.getQuestionIds();


        assertAll("Post round game state check",
                () -> assertEquals(player2.getId(), actualPlayerTurn,
                        "Should be player2's turn, but is not"),
                () -> assertEquals(0, gameStateResponse.getCurrentQuestionIndex(),
                        "Player 2 should start from index 0"),
                () -> assertEquals(GameStatus.ACTIVE, gameStateResponse.getStatus(),
                        "Game should be ACTIVE"),

                () -> assertEquals(3, actualPlayerDTO.getScore(),
                        "Player1 score should be 3"),
                () -> assertEquals(3, actualPlayerDTO.getQuestionsAnswered(),
                        "Player1 should have answered 3 questions"),

                () -> assertEquals(0, actualOpponentDTO.getScore(),
                        "Player2 score should be 0"),
                () -> assertEquals(0, actualOpponentDTO.getQuestionsAnswered(),
                        "Player2 should not have answered any questions yet"),

                () -> assertTrue(actualQuestionIds.containsAll(expectedQuestionIds),
                        "Expected question ids do not match the actual ids"),
                () -> assertEquals(1, gameStateResponse.getRoundCategories().size(),
                        "Category size should be 1"),
                () -> assertEquals("Sports", gameStateResponse.getRoundCategories().get(0),
                        "Played category should be sports")
        );

    }

    

    private void validateCorrectAnswer(Question question, Long sessionId) {
        MultiplayerAnswerValidationRequest request = new MultiplayerAnswerValidationRequest(
                question.getId(),
                sessionId,
                question.getCorrectAnswer(),
                player1.getId()
        );

        multiplayerQuestionService.validateMultiplayerAnswer(request);

    }
    
    @Nested
    class turnValidationTests {
        Long sessionId;
        List<Question> questions;
        @BeforeEach
        void setUp() {
            MultiplayerSession multiplayerSession = new MultiplayerSession(player1, player2, player1.getId());
            sessionId = multiplayerSessionRepository.save(multiplayerSession).getId();
            
            questions = loadQuestionsToDB();
        }
        
        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenOtherPlayersTurn() {
            GameException exception = assertThrows(GameException.class,
                    () -> multiplayerQuestionService.getNewQuestionsForCategory(
                            new MultiplayerQuestionsRequest("Geography", sessionId, player2.getId())
                    ), "Player should not be able to request questions when it's the other player's turn"
            );

            assertEquals(BusinessErrorCodes.NOT_PLAYER_TURN, exception.getErrorCode());
        }

        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenPlayerHasAnsweredMoreThanOpponent() {
            MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                    .orElseThrow();
            
            session.getSessionPlayers().get(0).setQuestionsAnswered(3);
            session.getSessionPlayers().get(1).setQuestionsAnswered(1);
            
            session.getQuestionIds().clear();

            multiplayerSessionRepository.save(session);
            
            MultiplayerQuestionsRequest request = new MultiplayerQuestionsRequest(
                    "Geography", session.getId(), player1.getId());

            GameException exception = assertThrows(GameException.class,
                    () -> gameValidationService.validatePlayerTurn(request, session),
                    "Should throw exception when player tries to request questions after answering more than opponent"
            );

            assertEquals(BusinessErrorCodes.MUST_WAIT_FOR_OPPONENT, exception.getErrorCode());
        }
        
        
        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenExistingQuestionsUnanswered() {
            multiplayerQuestionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));

            GameException exception = assertThrows(GameException.class,
                    () -> multiplayerQuestionService.getNewQuestionsForCategory(
                            new MultiplayerQuestionsRequest("Geography", sessionId, player1.getId())
                    ), "Should throw exception when player tries to select another category when they have finished the current"
            );
            assertEquals(BusinessErrorCodes.MUST_ANSWER_EXISTING_QUESTIONS, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenOtherPlayersTurn() {
            GameException exception = assertThrows(GameException.class,
                    () -> multiplayerQuestionService.validateMultiplayerAnswer(
                            new MultiplayerAnswerValidationRequest(questions.get(0).getId(), sessionId, questions.get(0).getCorrectAnswer(), player2.getId())
                    ), "Player should not be able to validate answer when it's the other player's turn"
            );

            assertEquals(BusinessErrorCodes.NOT_PLAYER_TURN, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenInvalidQuestionId() {
            multiplayerQuestionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));

            Long invalidQuestionId = 1231313213L;
            GameException exception = assertThrows(GameException.class,
                    () -> multiplayerQuestionService.validateMultiplayerAnswer(
                            new MultiplayerAnswerValidationRequest(invalidQuestionId, sessionId, "correctAnswer", player1.getId())
                    ), "Player should not be able to answer a question outside of the three round questions"
            );
            
            assertEquals(BusinessErrorCodes.INVALID_QUESTION, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenAlreadyAnsweredQuestion() {
            multiplayerQuestionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));
            
            MultiplayerAnswerValidationRequest request = 
                    new MultiplayerAnswerValidationRequest(questions.get(0).getId(), sessionId, questions.get(0).getCorrectAnswer(), player1.getId());
            
            multiplayerQuestionService.validateMultiplayerAnswer(request);
            
            GameException exception = assertThrows(GameException.class,
                    () ->  multiplayerQuestionService.validateMultiplayerAnswer(request),
                    "Player should not be able to answer the same question more than once"
            );
            assertEquals(BusinessErrorCodes.QUESTION_ALREADY_ANSWERED, exception.getErrorCode());
        }
    }

    @Nested
    class gameCompletionTests {
        private List<Question> questions;
        private List<Long> questionIds;

        @BeforeEach
        void setUp() {
            questions = loadQuestionsToDB();

             questionIds = questions.stream()
                    .map(Question::getId)
                    .toList();

            roundSessionService.initializeSession(player1.getId(), questions.stream().map(Question::getId).toList(), "Sports", RoundType.MULTIPLAYER);

        }

        @Test
        void testGameCompletionWin() {

            MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(createAlmostCompleteGame(15, 5));

            gameService.updateSessionQuestionsAndCategory(multiplayerSession, questionIds, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = gameService.getGameState(multiplayerSession.getId(), player1.getId());

            assertAll("Post-win checks",
                    () -> assertEquals(GameStatus.COMPLETED, gameStateResponse.getStatus(),
                            "Game status should be COMPLETED after a player wins."),
                    () -> assertEquals(player1.getId(), gameStateResponse.getWinnerId(),
                            "Player 1 should be marked as the winner."),
                    () -> assertEquals(player2.getId(), gameStateResponse.getLoserId(),
                            "Player 2 should be marked as the loser.")
            );

            verifyNotifications(NotificationType.GAME_WON, NotificationType.GAME_LOST);
        }
        
        @Test
        void testGameCompleteLoss() {
            MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(createAlmostCompleteGame(5, 18));

            gameService.updateSessionQuestionsAndCategory(multiplayerSession, questionIds, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = gameService.getGameState(multiplayerSession.getId(), player1.getId());

            assertAll("Post-win checks",
                    () -> assertEquals(GameStatus.COMPLETED, gameStateResponse.getStatus(),
                            "Game status should be COMPLETED after a player wins."),
                    () -> assertEquals(player1.getId(), gameStateResponse.getLoserId(),
                            "Player 1 should be marked as the loser."),
                    () -> assertEquals(player2.getId(), gameStateResponse.getWinnerId(),
                            "Player 2 should be marked as the winner.")
            );
            
            verifyNotifications(NotificationType.GAME_LOST, NotificationType.GAME_WON);
        }
        
        @Test
        void testGameCompleteTie() {
            MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(createAlmostCompleteGame(15, 18));

            gameService.updateSessionQuestionsAndCategory(multiplayerSession, questionIds, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = gameService.getGameState(multiplayerSession.getId(), player1.getId());

            assertAll("Post-tie checks",
                    () -> assertEquals(GameStatus.COMPLETED, gameStateResponse.getStatus(),
                            "Game status should be COMPLETED after a player wins."),
                    () -> assertNull(gameStateResponse.getLoserId(),
                            "Loser id should be null"),
                    () -> assertNull( gameStateResponse.getWinnerId(),
                            "Winner id should be null"),
                    () -> assertTrue(gameStateResponse.getIsTie(), "Game should be tied")
            );
            verifyNotifications(NotificationType.GAME_TIED, NotificationType.GAME_TIED);
        }
        
        @Test
        void testGameOverAcknowledgement() {
            MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(createAlmostCompleteGame(15, 5));

            gameService.updateSessionQuestionsAndCategory(multiplayerSession, questionIds, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));
            
            gameService.acknowledgeGameOver(multiplayerSession.getId(), player1.getId());

            GameStateResponse firstState = gameService.getGameState(multiplayerSession.getId(), player1.getId());
            assertAll("First acknowledgement checks",
                    () -> assertTrue(firstState.getPlayerDTO().isHasAcknowledgedGameOver(),
                            "Player1 acknowledgement should be true"),
                    () -> assertFalse(firstState.getOpponentDTO().isHasAcknowledgedGameOver(),
                            "Player2 acknowledgement should be false")
            );
            
            gameService.acknowledgeGameOver(multiplayerSession.getId(), player2.getId());

            GameStateResponse secondState = gameService.getGameState(multiplayerSession.getId(), player1.getId());
            assertAll("Second acknowledgement checks",
                    () -> assertTrue(secondState.getPlayerDTO().isHasAcknowledgedGameOver(),
                            "Player1 acknowledgement should be true"),
                    () -> assertTrue(secondState.getOpponentDTO().isHasAcknowledgedGameOver(),
                            "Player2 acknowledgement should be true")
            );
        }

        private MultiplayerSession createAlmostCompleteGame(int player1Score, int player2Score) {
            MultiplayerSession session = new MultiplayerSession(player1, player2, player1.getId());
            
            session.getSessionPlayers().get(0).setQuestionsAnswered(15);
            session.getSessionPlayers().get(0).setScore(player1Score);

            session.getSessionPlayers().get(1).setQuestionsAnswered(18);
            session.getSessionPlayers().get(1).setScore(player2Score);
            
            session.setCurrentPlayerTurnId(player1.getId());
            session.setStatus(GameStatus.ACTIVE);

            return session;
        }
    }
    
    @Test
    void testGiveUpHandling() {
        MultiplayerSession multiplayerSession = new MultiplayerSession(player1, player2, player1.getId());
        multiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(multiplayerSession);
        
        gameService.handleGiveUp(multiplayerSession.getId(), player1.getId());
        
        MultiplayerSession updatedSession = multiplayerSessionRepository.findById(multiplayerSession.getId())
                .orElseThrow();

        SessionPlayer updatedPlayer1 = updatedSession.getSessionPlayers().stream()
                .filter(player -> player.getPlayer().getId().equals(player1.getId()))
                .findFirst()
                .orElseThrow();

        assertAll("Post give up session checks",
                () -> assertTrue(updatedPlayer1.isGivenUp(), "Player1 should be marked as given up"),
                () -> assertEquals(GameStatus.COMPLETED, updatedSession.getStatus(), "Game status should be COMPLETED"),
                () -> assertEquals(player1.getId(), updatedSession.getLoserId(), "Player1 should be the loser"),
                () -> assertEquals(player2.getId(), updatedSession.getWinnerId(), "Player2 should be the winner")
        );
        
        verifyNotifications(NotificationType.GAME_LOST, NotificationType.GAME_WON);
    }
    

    private List<Question> loadQuestionsToDB() {
        Question questionObj1 = new Question();
        questionObj1.setCategory("Sports");
        questionObj1.setQuestion("In Baseball, how many times does the ball have to be pitched outside of the strike zone before the batter is walked?");
        questionObj1.setOptions(Arrays.asList("4", "1", "2", "3"));
        questionObj1.setCorrectAnswer("4");

        Question questionObj2 = new Question();
        questionObj2.setCategory("Sports");
        questionObj2.setQuestion("What cricketing term denotes a batsman being dismissed with a score of zero?");
        questionObj2.setOptions(Arrays.asList("Duck", "Bye", "Beamer", "Carry"));
        questionObj2.setCorrectAnswer("Duck");

        Question questionObj3 = new Question();
        questionObj3.setCategory("Sports");
        questionObj3.setQuestion("In what sport does Fanny Chmelar compete for Germany?");
        questionObj3.setOptions(Arrays.asList("Skiing", "Swimming", "Showjumping", "Gymnastics"));
        questionObj3.setCorrectAnswer("Skiing");
        
        return questionRepository.saveAll(List.of(questionObj1, questionObj2, questionObj3));
    }

    private void verifyNotifications(NotificationType player1ExpectedType, NotificationType player2ExpectedType) {

        List<Notification> player1Notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player1.getId());
        List<Notification> player2Notifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId());

        assertAll("Post-game notification checks",
                () -> assertTrue(player1Notifications.size() == 1
                                && player1Notifications.get(0).getType().equals(player1ExpectedType),
                        String.format("Player 1 should have one unread %s notification.", player1ExpectedType)),
                () -> assertTrue(player2Notifications.size() == 1
                                && player2Notifications.get(0).getType().equals(player2ExpectedType),
                        String.format("Player 2 should have one unread %s notification.", player2ExpectedType))
        );
    }
}
