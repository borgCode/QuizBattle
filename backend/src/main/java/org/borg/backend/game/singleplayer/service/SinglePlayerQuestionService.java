package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.shared.dto.AnswerValidationResponse;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.borg.backend.game.shared.mapper.QuestionMapper;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.model.RoundType;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.GameValidationService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.game.singleplayer.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SinglePlayerQuestionService {
    private final QuestionRepository questionRepository;
    private final RoundSessionService roundSessionService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChapterSessionService chapterSessionService;
    private final ChapterProgressRepository chapterProgressRepository;
    private final ChapterService chapterService;
    private final GameValidationService gameValidationService;
    private final StatsService statsService;

    public List<QuestionDTO> getSinglePlayerRoundQuestions(long playerId) {
        log.debug("Fetching single player round questions for player ID: {}", playerId);
        
        ChapterSession session = chapterSessionService.getSession(playerId);
        String currentCategory = session.getCurrentCategory();
        log.debug("Current category for player {}: {}", playerId, currentCategory);

        List<Question> questions = questionRepository.findFiveRandomQuestionsByCategory(currentCategory);
        log.debug("Retrieved {} questions for category {}", questions.size(), currentCategory);

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        log.debug("Question IDs for round: {}", questionIds);

        roundSessionService.initializeSession(
                playerId,
                questionIds,
                currentCategory,
                RoundType.SINGLE_PLAYER
        );
        log.debug("Initialized round session for player {} with category {}", playerId, currentCategory);

        return QuestionMapper.multipleToDTO(questions);
    }
    
    public AnswerValidationResponse validateSingleplayerAnswer(SinglePlayerAnswerValidationRequest request) {
        log.debug("Validating singleplayer answer for request: {}", request);
        if (request == null) {
            throw new GameException(BusinessErrorCodes.NULL_REQUEST, "Request cannot be null");
        }

        gameValidationService.validateSinglePlayerAnswer(request.getPlayerId(), request.getQuestionId());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Question not found for " + request.getQuestionId()));

        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        boolean isRoundComplete = roundSessionService.saveAnswer(
                request.getPlayerId(),
                question.getId(),
                validationResponse.isCorrect());

        if (isRoundComplete) {
            applicationEventPublisher.publishEvent(
                    new AchievementEvents.CategoryCompletedEvent(
                            request.getPlayerId(),
                            roundSessionService.getCurrentCategory(request.getPlayerId())
                    )
            );
        }
        return validationResponse;
    }

    private AnswerValidationResponse validateAnswer(Long playerId, Question question, String answer) {
        boolean isCorrect = question.getCorrectAnswer().equals(answer);
        int indexOfCorrectAnswer = question.getOptions().indexOf(question.getCorrectAnswer());
        
        statsService.updateQuestionStats(playerId, question.getCategory(), isCorrect);

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }

    public ChapterRoundResults getRoundResults(Long playerId) {
        log.debug("Retrieving round results for player {}", playerId);
        
        List<Boolean> results = roundSessionService.getSessionAnswers(playerId);
        log.debug("Retrieved session answers for player {}: {}", playerId, results);
        
        roundSessionService.finishSession(playerId);
        log.debug("Finished round session for player {}", playerId);
        
        ChapterRoundResults roundResults = chapterSessionService.getRoundResults(playerId, results);

        if (roundResults.isChapterComplete() && !roundResults.isGameOver()) {
            ChapterProgress progress = chapterProgressRepository
                    .findByPlayerIdAndChapterId(playerId, chapterSessionService.getSession(playerId).getChapterId());
            chapterService.updateChapterProgress(progress.getId());
            chapterSessionService.clearSession(playerId);
        }
        return roundResults;
    }

    public void clearSession(long playerId) {
        log.debug("Clearing all sessions for player {}", playerId);
        roundSessionService.finishSession(playerId);
        chapterSessionService.clearSession(playerId);
        log.debug("Successfully cleared round and chapter sessions for player {}", playerId);
    }
}
