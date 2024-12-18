package org.borg.backend.question.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionSessionService {
    
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Boolean>> quizSessions  = new ConcurrentHashMap<>();

    public void saveAnswer(Long playerId, Integer questionIndex, Boolean answer) {
        quizSessions.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(questionIndex, answer);
    }

    public Map<Integer, Boolean> getSessionAnswers(Long playerId) {
        return quizSessions.get(playerId);
    }

    public void finishSession(Long playerId) {
        quizSessions.remove(playerId);
    }
}
