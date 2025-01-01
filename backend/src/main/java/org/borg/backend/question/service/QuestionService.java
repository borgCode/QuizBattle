package org.borg.backend.question.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.multiplayer.service.MultiplayerService;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.repository.QuestionRepository;
import org.borg.backend.question.dto.*;
import org.borg.backend.question.mapper.QuestionMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final MultiplayerService multiplayerService;
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final QuestionSessionService questionSessionService;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;


    public List<QuestionDTO> getPlayerSessionQuestions(Long playerId) {
        List<Long> questionIds = questionSessionService.getSessionQuestions(playerId);
        log.warn("Getting session questions");
        
        if (questionIds == null) {
            return Collections.emptyList();
        }
        return QuestionMapper.multipleToDTO(questionRepository.findAllById(questionIds));
    }
    
    public List<QuestionDTO> getNewQuestionsForCategory(MultiplayerQuestionsRequest request) {
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        questionSessionService.initializeSession(request.getPlayerId(), questionIds);

        multiplayerService.updateSessionQuestionsAndCategory(request.getSessionId(), questions, request.getCategory());

        return QuestionMapper.multipleToDTO(questions);
    }

    public List<QuestionDTO> getActiveSessionQuestions(Long sessionId, Long playerId) {
        List<Long> sessionQuestions = questionSessionService.getSessionQuestions(playerId);
        if (!sessionQuestions.isEmpty()) {
            log.info("Player {} has ongoing session, returning those questions", playerId);
            return QuestionMapper.multipleToDTO(questionRepository.findAllById(sessionQuestions));
        }
        
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        List<Long> questionIds = session.getQuestionIds();
        
        questionSessionService.initializeSession(playerId, questionIds);

        return QuestionMapper.multipleToDTO(questionRepository.findAllById(questionIds));
    }

    public AnswerValidationResponse validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        boolean isLastQuestion = questionSessionService.saveMultiplayerAnswer(request.getPlayerId(), validationResponse.isCorrect());
        if (isLastQuestion) {
            log.warn("Publishing category complete event");
            applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(request.getPlayerId(), question.getCategory()));
            questionSessionService.finishSession(request.getPlayerId());
        }

        multiplayerService.updateGameState(
                request.getSessionId(),
                request.getPlayerId(),
                request.getQuestionId(),
                validationResponse.isCorrect()
        );

        return validationResponse;

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

    

    public List<String> getThreeRandomCategories(Long sessionId) {
        log.warn("Session ID: " + sessionId);
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        List<String> allCategories = questionRepository.findAllCategories();

        for (String allCategory : allCategories) {
            log.warn(allCategory);
        }

        List<String> categoriesNotPlayed = allCategories.stream()
                .filter(category -> !session.getPlayedCategories().contains(category))
                .collect(Collectors.toList());

        Collections.shuffle(categoriesNotPlayed);

        return categoriesNotPlayed.stream()
                .limit(3)
                .collect(Collectors.toList());
    }
    

    public List<QuestionDTO> getSinglePlayerRoundQuestions(SingleplayerQuestionsRequest request) {
        List<Question> questions = questionRepository.findFiveRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        questionSessionService.initializeSession(request.getPlayerId(), questionIds);

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
}
