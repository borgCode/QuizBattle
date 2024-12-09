package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.model.*;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.player.Player;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.question.PlayerQuestionResult;
import org.borg.backend.question.Question;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiplayerService {

    private final MultiplayerSessionRepository multiplayerSessionRepository;

    public GameStateResponse getGameState(Long sessionId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        for (Long questionId : multiplayerSession.getQuestionIds()) {
            log.warn("Question ID: " + questionId);
        }

        return new GameStateResponse(
                multiplayerSession.getCurrentPlayerTurn().getId(),
                PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()),
                multiplayerSession.getCurrentQuestionIndex(),
                multiplayerSession.getScore(),
                multiplayerSession.getStatus(),
                multiplayerSession.getQuestionResults(),
                multiplayerSession.getQuestionIds(),
                multiplayerSession.getRoundCategories()
        );
    }

    public synchronized void updateGameState(Long sessionId, Long playerId, Long questionId, boolean isCorrect) {
        log.warn("Finding game session to update");
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        log.warn("Fetched game session: {}", session);

        //Increment score if answer was correct
        log.warn("Answered correctly? - {}", isCorrect);
        if (isCorrect) {
            log.warn("Answered correctly, updating score");
            Map<Long, Integer> scores = session.getScore();
            log.warn("Score before update: {}", scores.getOrDefault(playerId, 0));
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
            log.warn("Score after update: {}", scores.get(playerId));
        }

        log.warn("Updating questions answered");
        //Update questions answered
        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        log.warn("questinsAnswered before update: {}", questionsAnswered.getOrDefault(playerId, 0));
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);
        log.warn("questionsAnswer after update: {}", questionsAnswered.get(playerId));
        
        //Update which question out of the 18 is correct
        session.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, session.getQuestionsAnswered().get(playerId) - 1, isCorrect));
        log.warn("Set is empty? {}", session.getQuestionResults().isEmpty());
        //Find opponent in session
        Player opponent = session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));

        log.warn("Opponent is: {}", opponent.getDisplayName());

        if (isGameComplete(questionsAnswered)) {
            session.setStatus(GameStatus.COMPLETED);
        } else {
            //Update question index to manage turns
            log.warn("Updating question index");
            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);

            //Determine if it's the opponent's turn
            log.warn("Checking if it's opponents turn");
            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    log.warn("Setting player to opponent");
                    session.setCurrentPlayerTurn(opponent);
                } else {
                    session.getQuestionIds().clear();
                    log.warn("Round over but not opponents turn");
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
        log.warn("Finding game session to update");
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        log.warn("Fetched game session: {}", session);
        session.getQuestionIds().clear();
        for (Question question : questions) {
            log.warn("Adding question: ID = {}, Question = {}", question.getId(), question.getQuestion());
            session.getQuestionIds().add(question.getId());
        }
        session.getPlayedCategories().add(selectedCategory);
        log.warn("Added category: {}", selectedCategory);
        for (String playedCategory : session.getPlayedCategories()) {
            log.warn("Played categories: {}", playedCategory);
        }
        
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

}
            
