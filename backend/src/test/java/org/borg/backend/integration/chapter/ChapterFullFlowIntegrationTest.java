package org.borg.backend.integration.chapter;

import Config.TestDataLoader;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.game.singleplayer.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.game.singleplayer.service.ChapterService;
import org.borg.backend.game.singleplayer.service.ChapterSessionService;
import org.borg.backend.game.singleplayer.service.SinglePlayerQuestionService;
import org.borg.backend.player.listener.AchievementListener;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestDataLoader.class)
public class ChapterFullFlowIntegrationTest {

    @Autowired
    private ChapterProgressRepository chapterProgressRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private PlayerProgressRepository playerProgressRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private ChapterService chapterService;
    @Autowired
    private TestDataLoader testDataLoader;
    @Autowired
    private ChapterSessionService chapterSessionService;
    @Autowired
    private SinglePlayerQuestionService singlePlayerQuestionService;
    @Autowired
    private RoundSessionService roundSessionService;
    @Autowired
    private QuestionRepository questionRepository;
    
    @MockitoBean
    private AchievementListener achievementListener;
    @MockitoBean
    private StatsService statsService;
    
    Player player;
    Story story;
    PlayerProgress playerProgress;
    Chapter chapter;

    @BeforeAll
    void setUpOnce() {
        chapterProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        playerProgressRepository.deleteAll();
        playerRepository.deleteAll();
        storyRepository.deleteAll();

        player = testDataLoader.createTestPlayer();
    }

    @AfterAll
    void cleanUpAll() {
        chapterProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        playerProgressRepository.deleteAll();
        playerRepository.deleteAll();
        storyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        chapterProgressRepository.deleteAll();
        playerProgressRepository.deleteAll();
        testDataLoader.cleanup();
    }

    @BeforeEach
    void setUp() {
        chapter = testDataLoader.createTestChapter(new String[]{"Science & Nature", "Sports", "Geography"});
        
        story = storyRepository.findById(chapter.getStory().getId())
                .orElseThrow();

        playerProgress = playerProgressRepository.save(PlayerProgress.builder()
                .player(player)
                .story(story)
                .completedChapters(0)
                .progressStatus(ProgressStatus.NOT_STARTED)
                .build());

        chapterService.startChapter(new StartChapterRequest(player.getId(), story.getId(), chapter.getId()));
    }

    @Test
    void fullHappyPathFlow() {

        PlayerProgress savedProgress = playerProgressRepository.findByPlayerIdAndStoryId(player.getId(), story.getId());
        ChapterProgress chapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(savedProgress.getId(), chapter.getId());

        assertAll(
                () -> assertEquals(LocalDate.now(), savedProgress.getStartedAt(), "Started date should be set to today"),
                () -> assertEquals(ProgressStatus.IN_PROGRESS, savedProgress.getProgressStatus(), "Status should be IN_PROGRESS"),
                () -> assertEquals(chapter.getId(), savedProgress.getCurrentChapterId(), "Current chapter ID should match requested chapter"),
                () -> assertEquals(LocalDate.now(), chapterProgress.getStartedAt(), "Started date should be set to today"),
                () -> assertEquals(ProgressStatus.IN_PROGRESS, chapterProgress.getProgressStatus(), "Status should be IN_PROGRESS"),
                () -> assertNotNull(chapterSessionService.getSession(player.getId()), "Player should be in started chapter session")
        );

        List<QuestionDTO> questions = singlePlayerQuestionService.getSinglePlayerRoundQuestions(player.getId());
        List<Long> questionIds = questions.stream()
                .map(QuestionDTO::getId)
                .toList();

        assertNotNull(roundSessionService.getSessionQuestions(player.getId()), "Player should be in a started round session");

        List<Long> initializedQuestionIds = roundSessionService.getSessionQuestions(player.getId());

        assertEquals(questionIds, initializedQuestionIds, "Initialized ids should match player's ids");

        List<Question> fullQuestions = questionRepository.findAllById(initializedQuestionIds);

        for (int i = 0; i < fullQuestions.size(); i++) {
            singlePlayerQuestionService.validateSingleplayerAnswer(
                    new SinglePlayerAnswerValidationRequest(questions.get(i).getId(), fullQuestions.get(i).getCorrectAnswer(), player.getId()));
        }

        ChapterRoundResults results = singlePlayerQuestionService.getRoundResults(player.getId());

        assertAll("Post first round checks",
                () -> assertTrue(results.getQuestionResults().stream()
                        .allMatch(Boolean::booleanValue), "All results should be marked as true (correct)"),
                () -> assertEquals(3, results.getCurrentHealth(), "Player should be at full health"),
                () -> assertFalse(results.isGameOver(), "Game should not be over"),
                () -> assertFalse(results.isChapterComplete(), "Chapter should not be complete"),
                () -> assertTrue(results.isRoundPassed(), "Round should be passed")
        );

        playTwoRounds();

        ChapterRoundResults resultsAfterLastRound = singlePlayerQuestionService.getRoundResults(player.getId());

        assertAll("Post last round checks",
                () -> assertTrue(resultsAfterLastRound.isChapterComplete(), "Chapter should be complete"),
                () -> assertTrue(resultsAfterLastRound.isRoundPassed(), "Round should be passed")
        );

        ChapterProgress updatedChapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(playerProgress.getId(), player.getId());
        PlayerProgress playerProgress = playerProgressRepository.findById(chapterProgress.getPlayerProgress().getId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player progress not found for " + chapterProgress.getPlayerProgress().getId()));

        assertAll("Progress updates after chapter complete",
                () -> assertEquals(ProgressStatus.COMPLETED, updatedChapterProgress.getProgressStatus(), "Chapter should be marked as complete"),
                () -> assertEquals(1, playerProgress.getCompletedChapters(), "Player progress should have one chapter completed"));
    }

    private void playTwoRounds() {
        for (int i = 0; i < 2; i++) {

            List<QuestionDTO> questions = singlePlayerQuestionService.getSinglePlayerRoundQuestions(player.getId());

            List<Long> initializedQuestionIds = roundSessionService.getSessionQuestions(player.getId());

            List<Question> fullQuestions = questionRepository.findAllById(initializedQuestionIds);

            for (int j = 0; j < fullQuestions.size(); j++) {
                singlePlayerQuestionService.validateSingleplayerAnswer(
                        new SinglePlayerAnswerValidationRequest(questions.get(j).getId(), fullQuestions.get(j).getCorrectAnswer(), player.getId()));
            }
            if (i < 1) {
                singlePlayerQuestionService.getRoundResults(player.getId());
            }
        }
    }

    @Test
    @Transactional
    void healthDepletionAndGameOver() {
        List<QuestionDTO> questions = singlePlayerQuestionService.getSinglePlayerRoundQuestions(player.getId());
        List<Long> initializedQuestionIds = roundSessionService.getSessionQuestions(player.getId());
        List<Question> fullQuestions = questionRepository.findAllById(initializedQuestionIds);

        for (int i = 0; i < fullQuestions.size(); i++) {
            singlePlayerQuestionService.validateSingleplayerAnswer(
                    new SinglePlayerAnswerValidationRequest(questions.get(i).getId(), fullQuestions.get(i).getOptions().get(2), player.getId()));
        }

        ChapterRoundResults results = singlePlayerQuestionService.getRoundResults(player.getId());

        assertAll("Post first round checks",
                () -> assertTrue(results.getQuestionResults().stream()
                        .noneMatch(Boolean::booleanValue), "All results should be marked as false (incorrect)"),
                () -> assertEquals(2, results.getCurrentHealth(), "Player should be at 2 health"),
                () -> assertFalse(results.isGameOver(), "Game should not be over"),
                () -> assertFalse(results.isRoundPassed(), "Round should not be passed")
        );

        playTwoFailedRounds();

        ChapterRoundResults resultsAfterLastRound = singlePlayerQuestionService.getRoundResults(player.getId());

        assertAll("Post last round checks",
                () -> assertFalse(resultsAfterLastRound.isChapterComplete(), "Chapter should not be complete"),
                () -> assertFalse(resultsAfterLastRound.isRoundPassed(), "Round should not be passed"),
                () -> assertTrue(resultsAfterLastRound.isGameOver(), "Game should be over"),
                () -> assertEquals(0, resultsAfterLastRound.getCurrentHealth(), "Health should be at 0")
        );
    }

    private void playTwoFailedRounds() {
        for (int i = 0; i < 2; i++) {

            List<QuestionDTO> questions = singlePlayerQuestionService.getSinglePlayerRoundQuestions(player.getId());

            List<Long> initializedQuestionIds = roundSessionService.getSessionQuestions(player.getId());

            List<Question> fullQuestions = questionRepository.findAllById(initializedQuestionIds);

            for (int j = 0; j < fullQuestions.size(); j++) {
                singlePlayerQuestionService.validateSingleplayerAnswer(
                        new SinglePlayerAnswerValidationRequest(questions.get(j).getId(), fullQuestions.get(i).getOptions().get(2), player.getId()));
            }
            if (i < 1) {
                singlePlayerQuestionService.getRoundResults(player.getId());
            }
        }
    }
}
