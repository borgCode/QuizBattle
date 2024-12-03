package org.borg.backend.multiplayer;

import lombok.RequiredArgsConstructor;
import org.borg.backend.question.Question;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiplayerService {
    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    

    public MatchmakingResponse findMatch(MatchmakingRequest request) {
        synchronized (matchmakingQueue) {
            Optional<Long> opponent = matchmakingQueue.stream().findFirst();

            if (opponent.isPresent()) {
                matchmakingQueue.remove(opponent.get());

                //Randomly choose who starts
                Long startingPlayer = Math.random() < 0.5 ? request.getPlayerId() : opponent.get();

                MultiplayerSession session = new MultiplayerSession(request.getPlayerId(), opponent.get(), startingPlayer);
                multiplayerSessionRepository.save(session);
                return new MatchmakingResponse(MatchStatus.MATCHED, session.getId());
            }

            matchmakingQueue.add(request.getPlayerId());
            return new MatchmakingResponse(MatchStatus.WAITING, null);
        }
    }

    public void cancelMatchmaking(Long playerId) {
        matchmakingQueue.remove(playerId);
    }

    public GameStateResponse getGameState(Long sessionId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        return new GameStateResponse(
                multiplayerSession.getPlayerIds(),
                multiplayerSession.getCurrentQuestionIndex(),
                multiplayerSession.getScore(),
                multiplayerSession.getStatus()
        );
    }

    public synchronized GameStateUpdate updateGameState(Long sessionId, Long playerId, boolean isCorrect) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        if (isCorrect) {
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
        }
        
        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);

        Long opponentId = null;
        for (Long id : questionsAnswered.keySet()) {
            if (!id.equals(playerId)) {
                opponentId = id;
                break;
            }
        }

        if (opponentId == null) {
            throw new IllegalStateException("No opponent found in session");
        }
        

        if (questionsAnswered.get(playerId) == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER
                && questionsAnswered.get(opponentId) == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER) {
            return new GameStateUpdate(
                    null,
                    session.getScore(),
                    GameStatus.COMPLETED,
                    null
            );
        }

        session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);
        
        boolean isOpponentTurn = false;
        if (session.getCurrentQuestionIndex() == 0) {
            if (questionsAnswered.get(playerId) > questionsAnswered.get(opponentId)) {
                session.setCurrentPlayerTurn(opponentId);
                isOpponentTurn = true;
                
            }
        }
        return new GameStateUpdate(
                session.getCurrentQuestionIndex(),
                session.getScore(),
                GameStatus.ACTIVE,
                isOpponentTurn
                );
    }

    public void updateSessionQuestions(Long sessionId, List<Question> questions) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
        }
        multiplayerSessionRepository.save(session);
    }
}
            
