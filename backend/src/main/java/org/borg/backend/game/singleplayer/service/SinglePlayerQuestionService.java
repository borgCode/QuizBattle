package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.shared.model.RoundType;
import org.borg.backend.game.shared.service.GameValidationService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.game.singleplayer.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.events.StatsEvents;
import org.borg.backend.game.shared.dto.AnswerValidationResponse;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.borg.backend.game.shared.mapper.QuestionMapper;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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

    public List<QuestionDTO> getSinglePlayerRoundQuestions(long playerId) {
        ChapterSession session = chapterSessionService.getSession(playerId);
        String currentCategory = session.getCurrentCategory();

        List<Question> questions = questionRepository.findFiveRandomQuestionsByCategory(currentCategory);

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        roundSessionService.initializeSession(
                playerId,
                questionIds,
                currentCategory,
                RoundType.SINGLE_PLAYER
        );

        return QuestionMapper.multipleToDTO(questions);
    }

    @Transactional
    public AnswerValidationResponse validateSingleplayerAnswer(SinglePlayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        gameValidationService.validateSinglePlayerAnswer(request.getPlayerId(), request.getQuestionId());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        boolean isRoundComplete = roundSessionService.saveAnswer(
                request.getPlayerId(),
                question.getId(),
                validationResponse.isCorrect()
        );

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

        applicationEventPublisher.publishEvent(new StatsEvents.QuestionAnsweredEvent(playerId, question.getCategory(), isCorrect));

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }
    

    public ChapterRoundResults getRoundResults(Long playerId) {
        List<Boolean> results = roundSessionService.getSessionAnswers(playerId);
        roundSessionService.finishSession(playerId);

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
        roundSessionService.finishSession(playerId);
        chapterSessionService.clearSession(playerId);
    }
}
