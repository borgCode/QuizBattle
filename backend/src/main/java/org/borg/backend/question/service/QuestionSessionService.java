package org.borg.backend.question.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QuestionSessionService {

    private final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Boolean>> quizSessions = new ConcurrentHashMap<>();

    public void saveAnswer(Long playerId, Integer questionIndex, Boolean isCorrect) {
        log.warn("Player: " + playerId + " answered " + isCorrect + " on index: " + questionIndex);
        ConcurrentHashMap<Integer, Boolean> playerAnswers = quizSessions.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        if (playerAnswers.size() >= 5) {
            throw new IllegalStateException("Player has already answered the maximum number of questions");
        }

        playerAnswers.put(questionIndex, isCorrect);
    }

    public List<Boolean> getSessionAnswers(Long playerId) {
        return quizSessions.get(playerId).values().stream().toList();
    }

    public void finishSession(Long playerId) {
        log.warn("Clearing round results");
        quizSessions.remove(playerId);
    }
}
