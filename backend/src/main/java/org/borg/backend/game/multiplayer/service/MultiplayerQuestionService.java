package org.borg.backend.game.multiplayer.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.*;
import org.borg.backend.question.mapper.QuestionMapper;
import org.borg.backend.question.repository.QuestionRepository;
import org.borg.backend.question.service.QuestionSessionService;
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
public class MultiplayerQuestionService {

    private final QuestionSessionService questionSessionService;
    private final QuestionRepository questionRepository;
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final GameService gameService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlayerRepository playerRepository;

    public List<QuestionDTO> restoreSessionQuestions(Long playerId) {
        List<Long> questionIds = questionSessionService.getSessionQuestions(playerId);
        log.warn("Getting session questions");

        if (questionIds == null) {
            return Collections.emptyList();
        }
        return QuestionMapper.multipleToDTO(questionRepository.findAllById(questionIds));
    }

    public List<String> getThreeRandomCategories(Long sessionId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        List<String> allCategories = questionRepository.findAllCategories();

        List<String> categoriesNotPlayed = allCategories.stream()
                .filter(category -> !session.getPlayedCategories().contains(category))
                .collect(Collectors.toList());

        Collections.shuffle(categoriesNotPlayed);

        return categoriesNotPlayed.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<QuestionDTO> getNewQuestionsForCategory(MultiplayerQuestionsRequest request) {

        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        gameService.validatePlayerTurn(request, session);

        String currentCategory = questionSessionService.getCurrentCategory(request.getPlayerId());
        if (currentCategory != null && currentCategory.equalsIgnoreCase(request.getCategory())) {
            return Collections.emptyList();
        }


        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        questionSessionService.initializeSession(request.getPlayerId(), questionIds, request.getCategory());

        gameService.updateSessionQuestionsAndCategory(session, questions, request.getCategory());

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

        List<Question> questions = questionRepository.findAllById(questionIds);

        questionSessionService.initializeSession(playerId, questionIds, questions.get(0).getCategory());

        return QuestionMapper.multipleToDTO(questions);
    }

    @Transactional
    public AnswerValidationResponse validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }


        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        if (!session.getCurrentPlayerTurn().getId().equals(request.getPlayerId())) {
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN);
        }

        if (!session.getQuestionIds().contains(request.getQuestionId())) {
            throw new GameException(BusinessErrorCodes.INVALID_QUESTION);
        }


        if (questionSessionService.isQuestionAnswered(request.getPlayerId(), request.getQuestionId())) {
            log.warn("Attempt to answer already answered question: {}", request.getQuestionId());
            throw new GameException(BusinessErrorCodes.QUESTION_ALREADY_ANSWERED);
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        boolean isLastQuestion = questionSessionService.saveMultiplayerAnswer(request.getPlayerId(), request.getQuestionId(), validationResponse.isCorrect());
        if (isLastQuestion) {
            log.warn("Publishing category complete event");
            applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(request.getPlayerId(), question.getCategory()));

            questionSessionService.finishSession(request.getPlayerId());
        }

        gameService.updateGameState(
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

}
