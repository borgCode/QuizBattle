package org.borg.backend.game.singleplayer;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.game.shared.RoundType;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.AnswerValidationResponse;
import org.borg.backend.question.dto.QuestionDTO;
import org.borg.backend.question.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.question.dto.SingleplayerQuestionsRequest;
import org.borg.backend.question.mapper.QuestionMapper;
import org.borg.backend.question.model.Question;
import org.borg.backend.question.repository.QuestionRepository;
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
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public List<QuestionDTO> getSinglePlayerRoundQuestions(SingleplayerQuestionsRequest request) {
        List<Question> questions = questionRepository.findFiveRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        roundSessionService.initializeSession(
                request.getPlayerId(),
                questionIds,
                request.getCategory(),
                RoundType.SINGLE_PLAYER
        );

        return QuestionMapper.multipleToDTO(questions);
    }

    @Transactional
    public AnswerValidationResponse validateSingleplayerAnswer(SinglePlayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

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

        updatePlayerStats(playerId, question.getCategory(), isCorrect);

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }

    private void updatePlayerStats(Long playerId, String category, boolean isCorrect) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        player.getStats().incrementQuestionsAnswered(category);

        if (isCorrect) {
            player.getStats().incrementCorrectAnswer(category);
        }

        playerRepository.save(player);
    }

    public List<Boolean> getRoundResults(Long playerId) {
        List<Boolean> results = roundSessionService.getSessionAnswers(playerId);
        roundSessionService.finishSession(playerId);
        return results;
    }
}
