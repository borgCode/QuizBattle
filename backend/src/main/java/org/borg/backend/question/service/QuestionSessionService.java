package org.borg.backend.question.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QuestionSessionService {

    private final ConcurrentHashMap<Long, PlayerSession> quizSessions = new ConcurrentHashMap<>();
    

    @Data
    @AllArgsConstructor
    private static class PlayerSession {
        private List<Long> questionIds;
        private ConcurrentHashMap<Integer, Boolean> answers;
        private LocalDateTime timestamp;
        private int currentIndex;
    }
    
    public List<Long> initializeSession(Long playerId, List<Long> questionIds) {
        if (quizSessions.containsKey(playerId)) {
            return quizSessions.get(playerId).getQuestionIds();
        }
        
        PlayerSession session = new PlayerSession(questionIds, new ConcurrentHashMap<>(), LocalDateTime.now(), 0);
        
        quizSessions.put(playerId, session);
        
        return questionIds;
    }
    
    public boolean saveMultiplayerAnswer(Long playerId, Boolean isCorrect) {
        PlayerSession session = quizSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active sessions found for player");
        }

        ConcurrentHashMap<Integer, Boolean> playerAnswers = session.getAnswers();
        
        if (playerAnswers.size() >= 3) {
            throw new IllegalStateException("Player has already answered the maximum number of questions");
        }

        playerAnswers.put(session.getCurrentIndex(), isCorrect);
        session.setCurrentIndex(session.getCurrentIndex() + 1);

        return playerAnswers.size() >= 3;
    }

    public void saveSingleplayerAnswer(Long playerId, Boolean isCorrect) {
        log.warn("Player: " + playerId + " answered " + isCorrect + " on index: " + quizSessions.get(playerId).getCurrentIndex());

        PlayerSession session = quizSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active sessions found for player");
        }
        
        ConcurrentHashMap<Integer, Boolean> playerAnswers = session.getAnswers();

        if (playerAnswers.size() >= 5) {
            throw new IllegalStateException("Player has already answered the maximum number of questions");
        }

        playerAnswers.put(session.getCurrentIndex(), isCorrect);
        session.setCurrentIndex(session.getCurrentIndex() + 1);
    }

    public List<Boolean> getSessionAnswers(Long playerId) {
        PlayerSession session = quizSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active session found for player");
        }
        return session.getAnswers().values().stream().toList();
    }
    
    public List<Long> getSessionQuestions(Long playerId) {
        PlayerSession session = quizSessions.get(playerId);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.getQuestionIds();
    }

    public void finishSession(Long playerId) {
        log.warn("Clearing round results");
        quizSessions.remove(playerId);
    }
}

