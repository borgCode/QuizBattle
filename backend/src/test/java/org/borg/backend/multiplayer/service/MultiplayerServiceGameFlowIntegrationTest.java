package org.borg.backend.multiplayer.service;

import org.borg.backend.achievement.service.AchievementService;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.multiplayer.dto.GameStateResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.question.dto.PlayerQuestionResult;
import org.borg.backend.question.dto.Question;
import org.borg.backend.question.repository.QuestionRepository;
import org.borg.backend.question.service.QuestionService;
import org.borg.backend.question.service.QuestionSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private MultiplayerSession multiplayerSession;
    

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

        multiplayerSession = new MultiplayerSession(player1, player2, player1);
        multiplayerSession.setStatus(GameStatus.ACTIVE);
        multiplayerSessionRepository.save(multiplayerSession);
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
        List<Question> questions = loadQuestionsToDB();

        List<Long> expectedQuestionIds = questions.stream()
                .map(Question::getId)
                .toList();

        questionSessionService.initializeSession(player1.getId(), expectedQuestionIds, "Sports");
        multiplayerService.updateSessionQuestionsAndCategory(multiplayerSession, questions, "Sports");

        questions.forEach(question -> validateCorrectAnswer(question));

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

    private void validateCorrectAnswer(Question question) {
        MultiplayerAnswerValidationRequest request = new MultiplayerAnswerValidationRequest(
                question.getId(),
                multiplayerSession.getId(),
                question.getCorrectAnswer(),
                player1.getId()
        );

        questionService.validateMultiplayerAnswer(request);

    }

    @Test
    void testScoring() {
        // Test score updates and winner determination
    }

    @Test
    void testTurnValidation() {
        // Test turn order and validation rules
    }

    @Test
    void testRoundManagement() {
        // Test category selection and question progression
    }

    @Test
    void testGameCompletion() {
        // Test different game completion scenarios (win/loss/tie)
    }

    @Test
    void testPlayerAcknowledgment() {
        // Test game acknowledgment flow
    }
}
