package org.borg.backend.game.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.player.model.Player;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.springframework.stereotype.Service;

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
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN);
        }

        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        Long opponentId = session.getPlayers().stream()
                .filter(p -> !p.getId().equals(request.getPlayerId()))
                .findFirst()
                .map(Player::getId)
                .orElseThrow();

        if (questionsAnswered.get(request.getPlayerId()) > questionsAnswered.get(opponentId)) {
            throw new GameException(BusinessErrorCodes.MUST_WAIT_FOR_OPPONENT);
        }

        if (!session.getQuestionIds().isEmpty()) {
            throw new GameException(BusinessErrorCodes.MUST_ANSWER_EXISTING_QUESTIONS);
        }
    }
    
    public void validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request, MultiplayerSession session) {
        if (!session.getCurrentPlayerTurn().getId().equals(request.getPlayerId())) {
            throw new GameException(BusinessErrorCodes.NOT_PLAYER_TURN);
        }

        if (!session.getQuestionIds().contains(request.getQuestionId())) {
            throw new GameException(BusinessErrorCodes.INVALID_QUESTION);
        }
        
        validateIfQuestionAnswered(request.getPlayerId(), request.getQuestionId());
    }

    private void validateIfQuestionAnswered(Long playerId, Long questionId) {
        if (roundSessionService.isQuestionAnswered(playerId, questionId)) {
            log.warn("Attempt to answer already answered question: {}", questionId);
            throw new GameException(BusinessErrorCodes.QUESTION_ALREADY_ANSWERED);
        }
    }

}
