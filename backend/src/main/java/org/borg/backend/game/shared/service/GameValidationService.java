package org.borg.backend.game.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class GameValidationService {

    private final RoundSessionService roundSessionService;

    public GameValidationService(RoundSessionService roundSessionService) {
        this.roundSessionService = roundSessionService;
    }

    public void validatePlayerTurn(MultiplayerQuestionsRequest request, MultiplayerSession session) {
        log.debug("Validating player turn for playerId: {}, sessionId: {}", request.getPlayerId(), session.getId());
        
        if (!session.getCurrentPlayerTurnId().equals(request.getPlayerId())) {
            log.warn("Invalid turn attempt - Player {} tried to play during player {}'s turn",
                    request.getPlayerId(), session.getCurrentPlayerTurnId());
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN,
                    String.format("Player %d attempted to play but it's player %d's turn",
                            request.getPlayerId(), session.getCurrentPlayerTurnId()));
        }

        SessionPlayer sessionPlayer = session.getSessionPlayerByPlayerId(request.getPlayerId());
        SessionPlayer opponent = session.getOpponentSessionPlayerId(request.getPlayerId());

        if (sessionPlayer.getQuestionsAnswered() > opponent.getQuestionsAnswered()) {
            log.warn("Player {} (answers: {}) must wait for opponent {} (answers: {}) to catch up",
                    sessionPlayer.getPlayer().getId(), sessionPlayer.getQuestionsAnswered(),
                    opponent.getPlayer().getId(), opponent.getQuestionsAnswered());
            throw new GameException(BusinessErrorCodes.MUST_WAIT_FOR_OPPONENT,
                    String.format("Player %d must wait for opponent %d to catch up",
                            request.getPlayerId(), opponent.getPlayer().getId()));
        }

        if (!session.getQuestionIds().isEmpty()) {
            log.warn("Player {} attempted to request new questions while {} questions remain unanswered",
                    request.getPlayerId(), session.getQuestionIds().size());
            throw new GameException(BusinessErrorCodes.MUST_ANSWER_EXISTING_QUESTIONS,
                    String.format("Player %d must answer existing questions before requesting new ones",
                            request.getPlayerId()));
        }
        
        log.debug("Player turn validation successful for playerId: {}", request.getPlayerId());
    }

    public void validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request, MultiplayerSession session) {
        log.debug("Validating multiplayer answer - playerId: {}, questionId: {}, sessionId: {}",
                request.getPlayerId(), request.getQuestionId(), session.getId());

        if (!session.getCurrentPlayerTurnId().equals(request.getPlayerId())) {
            log.warn("Invalid answer attempt - Player {} tried to answer during Player {}'s turn",
                    request.getPlayerId(), session.getCurrentPlayerTurnId());
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN,
                    String.format("Player %d attempted to answer but it's not their turn",
                            request.getPlayerId()));
        }

        if (!session.getQuestionIds().contains(request.getQuestionId())) {
            log.warn("Invalid question attempt - Question {} is not part of session {} for player {}",
                    request.getQuestionId(), session.getId(), request.getPlayerId());
            throw new GameException(BusinessErrorCodes.INVALID_QUESTION,
                    String.format("Question %d is not part of the current session for player %d",
                            request.getQuestionId(), request.getPlayerId()));
        }

        validateIfQuestionAnswered(request.getPlayerId(), request.getQuestionId());
        log.debug("Multiplayer answer validation successful for playerId: {}, questionId: {}",
                request.getPlayerId(), request.getQuestionId());
    }

    public void validateSinglePlayerAnswer(Long playerId, Long questionId) {
        List<Long> sessionQuestions = roundSessionService.getSessionQuestions(playerId);
        if (sessionQuestions.isEmpty()) {
            log.warn("No active round session found for player: {}", playerId);
            throw new GameException(BusinessErrorCodes.NO_ACTIVE_SESSION,
                    String.format("No active round session found for player %d", playerId));
        }

        if (!sessionQuestions.contains(questionId)) {
            log.warn("Question {} not in current round session for player: {}", questionId, playerId);
            throw new GameException(BusinessErrorCodes.INVALID_QUESTION,
                    String.format("Question %d is not part of the current session for player %d",
                            questionId, playerId));
        }

        validateIfQuestionAnswered(playerId, questionId);
    }

    private void validateIfQuestionAnswered(Long playerId, Long questionId) {
        if (roundSessionService.isQuestionAnswered(playerId, questionId)) {
            log.warn("Attempt to answer already answered question: {}", questionId);
            throw new GameException(BusinessErrorCodes.QUESTION_ALREADY_ANSWERED,
                    String.format("Player %d attempted to answer already answered question %d",
                            playerId, questionId));
        }
    }
}
