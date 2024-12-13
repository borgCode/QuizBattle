package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.handler.BusinessErrorCodes;
import org.borg.backend.handler.GameException;
import org.borg.backend.multiplayer.model.*;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.notification.NotificationService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.question.PlayerQuestionResult;
import org.borg.backend.question.Question;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiplayerService {

    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final NotificationService notificationService;
    private final PendingSessionRepository pendingSessionRepository;

    public GameStateResponse getGameState(Long sessionId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        return new GameStateResponse(
                multiplayerSession.getCurrentPlayerTurn().getId(),
                PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()),
                multiplayerSession.getCurrentQuestionIndex(),
                multiplayerSession.getScore(),
                multiplayerSession.getStatus(),
                multiplayerSession.getQuestionResults(),
                multiplayerSession.getQuestionIds(),
                multiplayerSession.getRoundCategories(),
                multiplayerSession.getPlayerAcknowledgment()
        );
    }

    public synchronized void updateGameState(Long sessionId, Long playerId, Long questionId, boolean isCorrect) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        //Increment score if answer was correct
        if (isCorrect) {
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
        }
        
        //Update questions answered
        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);
        
        //Update which question out of the 18 is correct
        session.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, session.getQuestionsAnswered().get(playerId) - 1, isCorrect));
        //Find opponent in session
        Player opponent = session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));

        if (isGameComplete(questionsAnswered)) {
            session.setStatus(GameStatus.COMPLETED);
        } else {
            //Update question index to manage turns
            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);

            //Determine if it's the opponent's turn
            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    session.setCurrentPlayerTurn(opponent);
                } else {
                    session.getQuestionIds().clear();
                }
                
            }
        }

        multiplayerSessionRepository.save(session);

    }

    private boolean isGameComplete(Map<Long, Integer> questionsAnswered) {
        return questionsAnswered.values().stream()
                .allMatch(count -> count == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER);
    }

    public void updateSessionQuestionsAndCategory(Long sessionId, List<Question> questions, String selectedCategory) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
        }
        session.getPlayedCategories().add(selectedCategory);
        
        session.getRoundCategories().add(selectedCategory);
        multiplayerSessionRepository.save(session);
    }

    public List<MultiplayerSessionDTO> getMultiplayerSessionsById(Long playerId) {
        List<MultiplayerSession> multiplayerSessions = multiplayerSessionRepository.findByPlayerId(playerId);
        List<MultiplayerSessionDTO> multiplayerSessionDTOS = new ArrayList<>();
        for (MultiplayerSession multiplayerSession : multiplayerSessions) {
            MultiplayerSessionDTO multiplayerSessionDTO = MultiplayerSessionDTO.builder()
                    .id(multiplayerSession.getId())
                    .playerDTOList(PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()))
                    .score(multiplayerSession.getScore())
                    .status(multiplayerSession.getStatus())
                    .currentPlayerTurn(PlayerMapper.toDTO(multiplayerSession.getCurrentPlayerTurn()))
                    .build();
            multiplayerSessionDTOS.add(multiplayerSessionDTO);
        }
        return multiplayerSessionDTOS;
    }

    public void acknowledgeGameOver(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));
        
        session.getPlayerAcknowledgment().put(playerId, true);
        multiplayerSessionRepository.save(session);
    }

    @Transactional
    public void requestRematch(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        Map<Long, Boolean> playerWantsRematch = session.getPlayerWantsRematch();
        if (playerWantsRematch.get(playerId)) {
            throw new GameException(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT);
        }
        playerWantsRematch.put(playerId, true);
        
        Long opponentId = playerWantsRematch.keySet().stream()
                .filter(id -> !id.equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));

        String playerDisplayName = session.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .map(Player::getDisplayName)
                .orElseThrow(() -> new IllegalStateException("Player not found in session"));
        
        if (playerWantsRematch.values().stream().allMatch(Boolean::booleanValue)) {
            createRematchSession(session);
            notificationService.sendRematchStartedNotification(opponentId, playerDisplayName);
        } else {
            
            PendingSession pendingSession = pendingSessionRepository.save(new PendingSession(playerId, opponentId));
            
            notificationService.sendRematchRequestNotification(opponentId, pendingSession.getId(), playerDisplayName);
        }
        
    }

    private void createRematchSession(MultiplayerSession session) {
        List<Player> players = session.getPlayers();
        
        Player startingPlayer = Math.random() < 0.5 ? players.get(0) : players.get(1);
        
        
        MultiplayerSession multiplayerSession = new MultiplayerSession(players.get(0), players.get(1), startingPlayer);
        multiplayerSessionRepository.save(multiplayerSession);
    }

    public void handleGiveUp(Long sessionId, Long playerId) {
        
    }
}
            
