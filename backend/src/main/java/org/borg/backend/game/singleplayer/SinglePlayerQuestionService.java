package org.borg.backend.game.singleplayer;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.*;
import org.borg.backend.question.mapper.QuestionMapper;
import org.borg.backend.question.repository.QuestionRepository;
import org.borg.backend.question.service.QuestionSessionService;
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
    private final QuestionSessionService questionSessionService;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public List<QuestionDTO> getSinglePlayerRoundQuestions(SingleplayerQuestionsRequest request) {
        List<Question> questions = questionRepository.findFiveRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        questionSessionService.initializeSession(request.getPlayerId(), questionIds, request.getCategory());

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

        questionSessionService.saveSingleplayerAnswer(request.getPlayerId(), validationResponse.isCorrect());


        return validationResponse;
    }

    public void finishSession(Long playerId) {
        log.warn("Getting current category: " + questionSessionService.getCurrentCategory(playerId));

        applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(playerId, questionSessionService.getCurrentCategory(playerId)));

        questionSessionService.finishSession(playerId);


    }

    private AnswerValidationResponse validateAnswer(Long playerId, Question question, String answer) {

        boolean isCorrect;
        if (answer == null) {
            isCorrect = false;
        } else {
            isCorrect = question.getCorrectAnswer().equals(answer);
        }
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
    
    
}
