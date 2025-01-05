package org.borg.backend.multiplayer.service;

import org.borg.backend.achievement.service.AchievementService;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.multiplayer.dto.GameStateResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.*;
import org.borg.backend.question.repository.QuestionRepository;
import org.borg.backend.question.service.QuestionService;
import org.borg.backend.question.service.QuestionSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MultiplayerServiceGameFlowIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Autowired
    private MultiplayerService multiplayerService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuestionSessionService questionSessionService;
    @MockitoBean
    private AchievementService achievementService;
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

        MultiplayerSession multiplayerSession = new MultiplayerSession(player1, player2, player1);
        multiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(multiplayerSession);

        List<Question> questions = loadQuestionsToDB();

        List<Long> expectedQuestionIds = questions.stream()
                .map(Question::getId)
                .toList();

        questionSessionService.initializeSession(player1.getId(), expectedQuestionIds, "Sports");
        multiplayerService.updateSessionQuestionsAndCategory(multiplayerSession, questions, "Sports");

        questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

        GameStateResponse gameStateResponse = multiplayerService.getGameState(multiplayerSession.getId());


        List<Long> expectedPlayerIds = List.of(player1.getId(), player2.getId());

        List<Long> actualPlayerIds = gameStateResponse.getPlayerDTOS().stream()
                .map(PlayerDTO::getId)
                .toList();

        List<Long> actualQuestionIds = gameStateResponse.getResults().stream()
                .map(PlayerQuestionResult::getQuestionId)
                .toList();


        assertAll("Post round game state check",
                () -> assertEquals(player2.getId(), gameStateResponse.getPlayerTurn(), "Should be player2's turn, but is not"),
                () -> assertTrue(actualPlayerIds.containsAll(expectedPlayerIds), "The expected player ids did not match the actual ids"),
                () -> assertEquals(0, gameStateResponse.getCurrentQuestionIndex(), "Player 2 should start from index 0"),
                () -> assertEquals(3, gameStateResponse.getScores().get(player1.getId()),
                        "Player1 did not get all three questions correct as they should have"),
                () -> assertEquals(GameStatus.ACTIVE, gameStateResponse.getStatus(), "Game should be ACTIVE"),
                () -> assertTrue(actualQuestionIds.containsAll(expectedQuestionIds), "Expected ids do not match the actual ids"),
                () -> assertEquals(1, gameStateResponse.getRoundCategories().size(), "Category size should be 1"),
                () -> assertEquals("Sports", gameStateResponse.getRoundCategories().get(0), "Played category should be sports")
        );

    }

    

    private void validateCorrectAnswer(Question question, Long sessionId) {
        MultiplayerAnswerValidationRequest request = new MultiplayerAnswerValidationRequest(
                question.getId(),
                sessionId,
                question.getCorrectAnswer(),
                player1.getId()
        );

        questionService.validateMultiplayerAnswer(request);

    }

    @Test
    void testScoring() {
        // Test score updates and winner determination
    }
    
    @Nested
    class turnValidationTests {
        Long sessionId;
        List<Question> questions;
        @BeforeEach
        void setUp() {
            MultiplayerSession multiplayerSession = new MultiplayerSession(player1, player2, player1);
            sessionId = multiplayerSessionRepository.save(multiplayerSession).getId();
            
            questions = loadQuestionsToDB();
        }
        
        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenOtherPlayersTurn() {
            GameException exception = assertThrows(GameException.class,
                    () -> questionService.getNewQuestionsForCategory(
                            new MultiplayerQuestionsRequest("Geography", sessionId, player2.getId())
                    ), "Player should not be able to request questions when it's the other player's turn"
            );

            assertEquals(BusinessErrorCodes.NOT_PLAYER_TURN, exception.getErrorCode());
        }

        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenPlayerHasAnsweredMoreThanOpponent() {
            MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                    .orElseThrow();
            
            
            Map<Long, Integer> questionsAnswered = new HashMap<>(Map.of(
                    player1.getId(), 3,  
                    player2.getId(), 1  
            ));
            session.setQuestionsAnswered(questionsAnswered);
            
            session.getQuestionIds().clear();

            multiplayerSessionRepository.save(session);
            
            MultiplayerQuestionsRequest request = new MultiplayerQuestionsRequest(
                    "Geography", session.getId(), player1.getId());

            GameException exception = assertThrows(GameException.class,
                    () -> multiplayerService.validatePlayerTurn(request, session),
                    "Should throw exception when player tries to request questions after answering more than opponent"
            );

            assertEquals(BusinessErrorCodes.MUST_WAIT_FOR_OPPONENT, exception.getErrorCode());
        }
        
        
        @Test
        void shouldThrowErrorWhenRequestingQuestions_whenExistingQuestionsUnanswered() {
            questionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));

            GameException exception = assertThrows(GameException.class,
                    () -> questionService.getNewQuestionsForCategory(
                            new MultiplayerQuestionsRequest("Geography", sessionId, player1.getId())
                    ), "Should throw exception when player tries to select another category when they have finished the current"
            );
            assertEquals(BusinessErrorCodes.MUST_ANSWER_EXISTING_QUESTIONS, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenOtherPlayersTurn() {
            GameException exception = assertThrows(GameException.class,
                    () -> questionService.validateMultiplayerAnswer(
                            new MultiplayerAnswerValidationRequest(questions.get(0).getId(), sessionId, questions.get(0).getCorrectAnswer(), player2.getId())
                    ), "Player should not be able to validate answer when it's the other player's turn"
            );

            assertEquals(BusinessErrorCodes.NOT_PLAYER_TURN, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenInvalidQuestionId() {
            questionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));

            Long invalidQuestionId = 1231313213L;
            GameException exception = assertThrows(GameException.class,
                    () -> questionService.validateMultiplayerAnswer(
                            new MultiplayerAnswerValidationRequest(invalidQuestionId, sessionId, "correctAnswer", player1.getId())
                    ), "Player should not be able to answer a question outside of the three round questions"
            );
            
            assertEquals(BusinessErrorCodes.INVALID_QUESTION, exception.getErrorCode());
        }
        
        @Test
        void shouldThrowErrorWhenRequestingAnswerValidation_whenAlreadyAnsweredQuestion() {
            questionService.getNewQuestionsForCategory(
                    new MultiplayerQuestionsRequest("Sports", sessionId, player1.getId()));
            
            MultiplayerAnswerValidationRequest request = 
                    new MultiplayerAnswerValidationRequest(questions.get(0).getId(), sessionId, questions.get(0).getCorrectAnswer(), player2.getId());
            
            questionService.validateMultiplayerAnswer(request);
            
            GameException exception = assertThrows(GameException.class,
                    () ->  questionService.validateMultiplayerAnswer(request),
                    "Player should not be able to answer the same question more than once"
            );
            
            assertEquals(BusinessErrorCodes.QUESTION_ALREADY_ANSWERED, exception.getErrorCode());
        }
        
    }
    
    @Test
    void testRoundManagement() {
        // Test category selection and question progression
    }

    @Nested
    class gameCompletionTests {
        private List<Question> questions;

        @BeforeEach
        void setUp() {
            questions = loadQuestionsToDB();

            questionSessionService.initializeSession(player1.getId(), questions.stream().map(Question::getId).toList(), "Sports");

        }

        @Test
        void testGameCompletionWin() {

            MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(createAlmostCompleteGame(15, 5));

            multiplayerService.updateSessionQuestionsAndCategory(multiplayerSession, questions, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = multiplayerService.getGameState(multiplayerSession.getId());

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

            multiplayerService.updateSessionQuestionsAndCategory(multiplayerSession, questions, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = multiplayerService.getGameState(multiplayerSession.getId());

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

            multiplayerService.updateSessionQuestionsAndCategory(multiplayerSession, questions, "Sports");

            questions.forEach(question -> validateCorrectAnswer(question, multiplayerSession.getId()));

            GameStateResponse gameStateResponse = multiplayerService.getGameState(multiplayerSession.getId());

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

        private MultiplayerSession createAlmostCompleteGame(int player1Score, int player2Score) {

            MultiplayerSession session = new MultiplayerSession(player1, player2, player1);
            Map<Long, Integer> questionAnswered = new HashMap<>(Map.of(player1.getId(), 15, player2.getId(), 18));
            Map<Long, Integer> scores = new HashMap<>(Map.of(player1.getId(), player1Score, player2.getId(), player2Score));
            session.setCurrentPlayerTurn(player1);
            session.setQuestionsAnswered(questionAnswered);
            session.setScore(scores);
            session.setStatus(GameStatus.ACTIVE);

            return session;
        }

        private void verifyNotifications(NotificationType player1ExpectedType, NotificationType player2ExpectedType) {
            
            List<Notification> player1Notifications = notificationRepository.findByPlayerIdAndIsReadFalse(player1.getId());
            List<Notification> player2Notifications = notificationRepository.findByPlayerIdAndIsReadFalse(player2.getId());

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


    @Test
    void testPlayerAcknowledgment() {
        // Test game acknowledgment flow
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
}
