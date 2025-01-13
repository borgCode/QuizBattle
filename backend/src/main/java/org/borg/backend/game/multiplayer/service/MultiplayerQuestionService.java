package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.shared.dto.AnswerValidationResponse;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.borg.backend.game.shared.mapper.QuestionMapper;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.model.RoundType;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.GameValidationService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiplayerQuestionService {
    private final RoundSessionService roundSessionService;
    private final QuestionRepository questionRepository;
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final GameService gameService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameValidationService gameValidationService;
    private final StatsService statsService;
    
    @Transactional
    public List<QuestionDTO> getNewQuestionsForCategory(MultiplayerQuestionsRequest request) {
        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for " + request.getSessionId()));

        gameValidationService.validatePlayerTurn(request, session);

        String currentCategory = roundSessionService.getCurrentCategory(request.getPlayerId());
        if (currentCategory != null && currentCategory.equalsIgnoreCase(request.getCategory())) {
            return List.of();
        }

        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        roundSessionService.initializeSession(request.getPlayerId(), questionIds, request.getCategory(), RoundType.MULTIPLAYER);

        gameService.updateSessionQuestionsAndCategory(session, questions, request.getCategory());

        return QuestionMapper.multipleToDTO(questions);
    }

    @Transactional
    public AnswerValidationResponse validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for " + request.getSessionId()));

        gameValidationService.validateMultiplayerAnswer(request, session);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Question not found for " + request.getQuestionId()));

        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        boolean isRoundComplete = roundSessionService.saveAnswer(
                request.getPlayerId(),
                request.getQuestionId(),
                validationResponse.isCorrect()
        );

        gameService.updateGameState(request.getSessionId(), request.getPlayerId(), request.getQuestionId(), validationResponse.isCorrect());

        if (isRoundComplete) {
            applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(request.getPlayerId(), question.getCategory())
            );
            roundSessionService.finishSession(request.getPlayerId());
        }

        return validationResponse;
    }

    private AnswerValidationResponse validateAnswer(Long playerId, Question question, String answer) {
        boolean isCorrect = question.getCorrectAnswer().equals(answer);
        int indexOfCorrectAnswer = question.getOptions().indexOf(question.getCorrectAnswer());
        
        statsService.updateQuestionStats(playerId, question.getCategory(), isCorrect);

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }

    public List<String> getThreeRandomCategories(Long sessionId, long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for " + sessionId));

        boolean playerExistsInSession = session.getPlayers().stream()
                .anyMatch(player -> player.getId().equals(playerId));

        if (!playerExistsInSession) {
            throw new AccessDeniedException("Not authorized to get categories for this session");
        }

        List<String> allCategories = questionRepository.findAllCategories();

        List<String> categoriesNotPlayed = allCategories.stream()
                .filter(category -> !session.getPlayedCategories().contains(category))
                .collect(Collectors.toList());

        Collections.shuffle(categoriesNotPlayed);

        return categoriesNotPlayed.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getActiveSessionQuestions(Long sessionId, Long playerId) {
        List<Long> sessionQuestions = roundSessionService.getSessionQuestions(playerId);
        if (!sessionQuestions.isEmpty()) {
            log.info("Player {} has ongoing session, returning those questions", playerId);
            return QuestionMapper.multipleToDTO(questionRepository.findAllById(sessionQuestions));
        }

        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for " + sessionId));

        boolean playerExistsInSession = session.getPlayers().stream()
                .anyMatch(player -> player.getId().equals(playerId));

        if (!playerExistsInSession) {
            throw new AccessDeniedException("Not authorized to get questions for this session");
        }

        List<Long> questionIds = session.getQuestionIds();

        List<Question> questions = questionRepository.findAllById(questionIds);

        roundSessionService.initializeSession(
                playerId,
                questionIds,
                questions.get(0).getCategory(),
                RoundType.MULTIPLAYER
        );

        return QuestionMapper.multipleToDTO(questions);
    }
}
