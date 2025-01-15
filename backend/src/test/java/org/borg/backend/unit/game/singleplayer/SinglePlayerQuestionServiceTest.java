package org.borg.backend.unit.game.singleplayer;

import org.borg.backend.game.shared.dto.QuestionDTO;
import org.borg.backend.game.shared.mapper.QuestionMapper;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.GameValidationService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.game.singleplayer.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.service.ChapterService;
import org.borg.backend.game.singleplayer.service.ChapterSession;
import org.borg.backend.game.singleplayer.service.ChapterSessionService;
import org.borg.backend.game.singleplayer.service.SinglePlayerQuestionService;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SinglePlayerQuestionServiceTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ChapterProgressRepository chapterProgressRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private ChapterSessionService chapterSessionService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private GameValidationService gameValidationService;
    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private ChapterService chapterService;

    private RoundSessionService roundSessionService;
    private SinglePlayerQuestionService singlePlayerQuestionService;
    List<Question> mockQuestions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roundSessionService = new RoundSessionService(questionRepository, questionMapper);
        singlePlayerQuestionService = new SinglePlayerQuestionService(
                questionRepository,
                roundSessionService,
                applicationEventPublisher,
                chapterSessionService,
                chapterProgressRepository,
                chapterService,
                gameValidationService,
                statsService,
                questionMapper
        );
    }

    @Nested
    class GetQuestionsTests {

        @Test
        void shouldGetSinglePlayerRoundQuestions() {
            Long playerId = 1L;

            mockQuestions = List.of(
                    createTestQuestion(1L, "Q1"),
                    createTestQuestion(2L, "Q2"),
                    createTestQuestion(3L, "Q3"),
                    createTestQuestion(4L, "Q4"),
                    createTestQuestion(5L, "Q5")
            );

            List<QuestionDTO> mockQuestionDTOs = mockQuestions.stream()
                    .map(q -> QuestionDTO.builder()
                            .id(q.getId())
                            .question(q.getQuestion())
                            .build())
                    .toList();

            Set<String> categories = Set.of("History");
            ChapterSession chapterSession = new ChapterSession(playerId, categories, 2);

            when(chapterSessionService.getSession(playerId)).thenReturn(chapterSession);
            when(questionRepository.findFiveRandomQuestionsByCategory("History")).thenReturn(mockQuestions);
            when(questionMapper.multipleToDTO(mockQuestions)).thenReturn(mockQuestionDTOs);

            List<QuestionDTO> result = singlePlayerQuestionService.getSinglePlayerRoundQuestions(playerId);

            assertEquals(5, result.size());

            List<Long> questionIds = roundSessionService.getSessionQuestions(playerId);
            assertTrue(questionIds.containsAll(List.of(1L, 2L, 3L, 4L, 5L)));

            verify(chapterSessionService).getSession(playerId);
            verify(questionRepository).findFiveRandomQuestionsByCategory("History");
        }

        @Test
        void shouldThrowErrorWhenSessionNotFound() {
            Long playerId = 1L;

            when(chapterSessionService.getSession(playerId))
                    .thenThrow(new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for player: " + playerId));

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> singlePlayerQuestionService.getSinglePlayerRoundQuestions(playerId)
            );

            assertEquals(BusinessErrorCodes.RESOURCE_NOT_FOUND, exception.getErrorCode());
            assertEquals("Session not found for player: " + playerId, exception.getMessage());

            verify(chapterSessionService).getSession(playerId);
            verifyNoMoreInteractions(chapterSessionService);
            verifyNoInteractions(questionRepository);
        }
    }

    @Nested
    class validateAnswerTests {

        @Test
        void validateSingleplayerAnswer_whenRequestIsNull_shouldThrowGameException() {
            SinglePlayerAnswerValidationRequest request = null;

            GameException exception = assertThrows(GameException.class,
                    () -> singlePlayerQuestionService.validateSingleplayerAnswer(request));

            assertEquals(BusinessErrorCodes.NULL_REQUEST, exception.getErrorCode());
            assertEquals("Request cannot be null", exception.getMessage());
        }

        @Test
        void shouldThrowErrorWhenNoActiveSession_OnValidateAnswerRequest() {
            Long playerId = 1L;
            Long questionId = 1L;
            SinglePlayerAnswerValidationRequest request = new SinglePlayerAnswerValidationRequest(questionId, "Answer", playerId);

            doThrow(new GameException(BusinessErrorCodes.NO_ACTIVE_SESSION,
                    String.format("No active round session found for player %d", playerId)))
                    .when(gameValidationService).validateSinglePlayerAnswer(playerId, questionId);

            GameException exception = assertThrows(
                    GameException.class,
                    () -> singlePlayerQuestionService.validateSingleplayerAnswer(request)
            );

            assertEquals(BusinessErrorCodes.NO_ACTIVE_SESSION, exception.getErrorCode());
            assertEquals("No active round session found for player " + playerId, exception.getMessage());
        }

        @Test
        void shouldThrowErrorWhenInvalidQuestionId_OnValidateAnswerRequest() {
            Long playerId = 1L;
            Long questionId = 1L;

            SinglePlayerAnswerValidationRequest request = new SinglePlayerAnswerValidationRequest(questionId, "Answer", playerId);

            doThrow(new GameException(BusinessErrorCodes.INVALID_QUESTION,
                    String.format("Question %d is not part of the current session for player %d", playerId, questionId)))
                    .when(gameValidationService).validateSinglePlayerAnswer(playerId, questionId);

            GameException exception = assertThrows(
                    GameException.class,
                    () -> singlePlayerQuestionService.validateSingleplayerAnswer(request)
            );

            assertEquals(BusinessErrorCodes.INVALID_QUESTION, exception.getErrorCode());
            assertEquals("Question " + questionId + " is not part of the current session for player " + playerId, exception.getMessage());

            verify(questionRepository, never()).findById(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    private Question createTestQuestion(long id, String questionText) {
        Question question = new Question();
        question.setId(id);
        question.setQuestion(questionText);
        question.setCategory("HISTORY");
        question.setCorrectAnswer("Answer" + id);
        question.setOptions(List.of("Answer" + id, "Wrong1", "Wrong2"));
        return question;
    }
}
