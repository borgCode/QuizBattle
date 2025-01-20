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
    private final QuestionMapper questionMapper;

    @Transactional
    public List<QuestionDTO> getNewQuestionsForCategory(MultiplayerQuestionsRequest request) {
        log.debug("Getting new questions for category. SessionId: {}, PlayerId: {}, Category: {}",
                request.getSessionId(), request.getPlayerId(), request.getCategory());

        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> {
                    log.error("Session not found for sessionId: {}", request.getSessionId());
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Session not found for " + request.getSessionId());
                });

        gameValidationService.validatePlayerTurn(request, session);

        String currentCategory = roundSessionService.getCurrentCategory(request.getPlayerId());
        if (currentCategory != null && currentCategory.equalsIgnoreCase(request.getCategory())) {
            log.debug("Category {} is already active for player {}", currentCategory, request.getPlayerId());
            return List.of();
        }
        
        log.debug("Fetching three random questions for category: {}", request.getCategory());
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(request.getCategory());

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        log.debug("Selected question IDs: {}", questionIds);

        log.debug("Initializing session for player: {}", request.getPlayerId());
        roundSessionService.initializeSession(request.getPlayerId(), questionIds, request.getCategory(), RoundType.MULTIPLAYER);

        gameService.updateSessionQuestionsAndCategory(session, questionIds, request.getCategory());

        List<QuestionDTO> questionDTOs = questionMapper.multipleToDTO(questions);
        log.debug("Returning {} questions for player {}", questionDTOs.size(), request.getPlayerId());
        return questionDTOs;
    }

    @Transactional
    public AnswerValidationResponse validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request) {
        if (request == null) {
            log.error("Received null validation request");
            throw new IllegalArgumentException("Request cannot be null");
        }

        log.debug("Validating multiplayer answer. SessionId: {}, PlayerId: {}, QuestionId: {}",
                request.getSessionId(), request.getPlayerId(), request.getQuestionId());

        MultiplayerSession session = multiplayerSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> {
                    log.error("Session not found for sessionId: {}", request.getSessionId());
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Session not found for " + request.getSessionId());
                });
        
        gameValidationService.validateMultiplayerAnswer(request, session);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> {
                    log.error("Question not found for questionId: {}", request.getQuestionId());
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Question not found for " + request.getQuestionId());
                });

        log.debug("Validating answer for question: {}", request.getQuestionId());
        AnswerValidationResponse validationResponse = validateAnswer(request.getPlayerId(), question, request.getAnswer());

        log.debug("Saving answer for player: {}, question: {}, correct: {}",
                request.getPlayerId(), request.getQuestionId(), validationResponse.isCorrect());
        boolean isRoundComplete = roundSessionService.saveAnswer(
                request.getPlayerId(),
                request.getQuestionId(),
                validationResponse.isCorrect()
        );

        gameService.updateGameState(request.getSessionId(), request.getPlayerId(), request.getQuestionId(), validationResponse.isCorrect());

        if (isRoundComplete) {
            log.info("Round completed for player: {}, category: {}", request.getPlayerId(), question.getCategory());
            applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(
                    request.getPlayerId(), question.getCategory())
            );
            roundSessionService.finishSession(request.getPlayerId());
        }
        return validationResponse;
    }

    private AnswerValidationResponse validateAnswer(Long playerId, Question question, String answer) {
        log.debug("Validating answer for player: {}, question: {}, provided answer: {}",
                playerId, question.getId(), answer);
        
        boolean isCorrect = question.getCorrectAnswer().equals(answer);
        int indexOfCorrectAnswer = question.getOptions().indexOf(question.getCorrectAnswer());

        log.debug("Answer validation result - correct: {}, index: {}", isCorrect, indexOfCorrectAnswer);
        statsService.updateQuestionStats(playerId, question.getCategory(), isCorrect);

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }

    public List<String> getThreeRandomCategories(Long sessionId, long playerId) {
        log.debug("Getting three random categories for sessionId: {}, playerId: {}", sessionId, playerId);

        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found for sessionId: {}", sessionId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Session not found for " + sessionId);
                });


        boolean playerExistsInSession = session.getSessionPlayerByPlayerId(playerId) != null;
        if (!playerExistsInSession) {
            log.warn("Unauthorized access attempt for sessionId: {} by playerId: {}", sessionId, playerId);
            throw new AccessDeniedException("Not authorized to get categories for this session");
        }

        List<String> allCategories = questionRepository.findAllCategories();
        log.debug("Found {} total categories", allCategories.size());

        List<String> categoriesNotPlayed = allCategories.stream()
                .filter(category -> !session.getPlayedCategories().contains(category))
                .collect(Collectors.toList());
        log.debug("Found {} unplayed categories", categoriesNotPlayed.size());

        Collections.shuffle(categoriesNotPlayed);

        List<String> selectedCategories = categoriesNotPlayed.stream()
                .limit(3)
                .collect(Collectors.toList());
        log.debug("Selected categories: {}", selectedCategories);

        return selectedCategories;
    }

    public List<QuestionDTO> getActiveSessionQuestions(Long sessionId, Long playerId) {
        log.debug("Getting active session questions for sessionId: {}, playerId: {}", sessionId, playerId);
        
        List<Long> sessionQuestions = roundSessionService.getSessionQuestions(playerId);
        if (!sessionQuestions.isEmpty()) {
            log.info("Player {} has ongoing session, returning those questions", playerId);
            return questionMapper.multipleToDTO(questionRepository.findAllById(sessionQuestions));
        }

        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found for sessionId: {}", sessionId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Session not found for " + sessionId);
                });
        
        boolean playerExistsInSession = session.getSessionPlayerByPlayerId(playerId) != null;
        if (!playerExistsInSession) {
            log.warn("Unauthorized access attempt for sessionId: {} by playerId: {}", sessionId, playerId);
            throw new AccessDeniedException("Not authorized to get questions for this session");
        }

        List<Long> questionIds = session.getQuestionIds();
        log.debug("Found {} questions in session", questionIds.size());

        List<Question> questions = questionRepository.findAllById(questionIds);

        log.debug("Initializing new session for player: {}", playerId);
        roundSessionService.initializeSession(
                playerId,
                questionIds,
                questions.get(0).getCategory(),
                RoundType.MULTIPLAYER
        );

        List<QuestionDTO> questionDTOs = questionMapper.multipleToDTO(questions);
        log.debug("Returning {} questions for player {}", questionDTOs.size(), playerId);
        return questionDTOs;
    }
}
