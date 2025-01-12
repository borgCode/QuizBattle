package org.borg.backend.game.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.player.model.Player;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GameValidationService {

    private final RoundSessionService roundSessionService;

    public GameValidationService(RoundSessionService roundSessionService) {
        this.roundSessionService = roundSessionService;
    }

    public void validatePlayerTurn(MultiplayerQuestionsRequest request, MultiplayerSession session) {
        if (!session.getCurrentPlayerTurn().getId().equals(request.getPlayerId())) {
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN,
                    String.format("Player %d attempted to play but it's player %d's turn",
                            request.getPlayerId(), session.getCurrentPlayerTurn().getId()));
        }

        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        Long opponentId = session.getPlayers().stream()
                .filter(p -> !p.getId().equals(request.getPlayerId()))
                .findFirst()
                .map(Player::getId)
                .orElseThrow();

        if (questionsAnswered.get(request.getPlayerId()) > questionsAnswered.get(opponentId)) {
            throw new GameException(BusinessErrorCodes.MUST_WAIT_FOR_OPPONENT,
                    String.format("Player %d must wait for opponent %d to catch up",
                            request.getPlayerId(), opponentId));
        }

        if (!session.getQuestionIds().isEmpty()) {
            throw new GameException(BusinessErrorCodes.MUST_ANSWER_EXISTING_QUESTIONS,
                    String.format("Player %d must answer existing questions before requesting new ones",
                            request.getPlayerId()));
        }
    }

    public void validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request, MultiplayerSession session) {
        if (!session.getCurrentPlayerTurn().getId().equals(request.getPlayerId())) {
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN,
                    String.format("Player %d attempted to answer but it's player %d's turn",
                            request.getPlayerId(), session.getCurrentPlayerTurn().getId()));
        }

        if (!session.getQuestionIds().contains(request.getQuestionId())) {
            throw new GameException(BusinessErrorCodes.INVALID_QUESTION,
                    String.format("Question %d is not part of the current session for player %d",
                            request.getQuestionId(), request.getPlayerId()));
        }

        validateIfQuestionAnswered(request.getPlayerId(), request.getQuestionId());
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
