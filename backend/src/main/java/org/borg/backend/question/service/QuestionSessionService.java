package org.borg.backend.question.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QuestionSessionService {

    private final ConcurrentHashMap<Long, PlayerSession> quizSessions = new ConcurrentHashMap<>();


    @Data
    private static class PlayerSession {
        private List<Long> questionIds;
        private ConcurrentHashMap<Integer, Boolean> answers;
        private Instant timestamp;
        private int currentIndex;
        private String currentCategory;
        private Set<Long> answeredQuestionIds;

        public PlayerSession(List<Long> questionIds, String currentCategory) {
            this.questionIds = questionIds;
            this.answers = new ConcurrentHashMap<>();
            this.timestamp = Instant.now();
            this.currentIndex = 0;
            this.currentCategory = currentCategory;
            this.answeredQuestionIds = ConcurrentHashMap.newKeySet();
        }
    }

    public List<Long> initializeSession(Long playerId, List<Long> questionIds, String category) {
        PlayerSession existingSession = quizSessions.get(playerId);

        if (existingSession != null) {
            if (existingSession.answers.size() < 3) {
                log.warn("Attempted to start new category while current category incomplete");
                return existingSession.getQuestionIds();
            }
            log.info("Round completed, removing session for player: {}", playerId);
            quizSessions.remove(playerId);
            return Collections.emptyList();
        }
        
        PlayerSession session = new PlayerSession(questionIds, category);
        quizSessions.put(playerId, session);

        return questionIds;
    }

    public boolean saveMultiplayerAnswer(Long playerId, Long questionId, Boolean isCorrect) {
        PlayerSession session = quizSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active sessions found for player");
        }

        if (session.answeredQuestionIds.contains(questionId)) {
            log.warn("Attempted to answer same question twice: {}", questionId);
            throw new IllegalStateException("Question has already been answered");
        }

        ConcurrentHashMap<Integer, Boolean> playerAnswers = session.getAnswers();

        if (playerAnswers.size() >= 3) {
            throw new IllegalStateException("Player has already answered the maximum number of questions");
        }

        playerAnswers.put(session.getCurrentIndex(), isCorrect);
        session.setCurrentIndex(session.getCurrentIndex() + 1);
        session.answeredQuestionIds.add(questionId);

        return playerAnswers.size() >= 3;
    }

    public boolean isQuestionAnswered(Long playerId, Long questionId) {
        PlayerSession session = quizSessions.get(playerId);
        return session != null && session.getAnsweredQuestionIds().contains(questionId);
    }

    public String getCurrentCategory(Long playerId) {
        PlayerSession session = quizSessions.get(playerId);
        return session != null ? session.getCurrentCategory() : null;
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
        if (session.getAnswers().size() >= 3) {
            quizSessions.remove(playerId);
            return Collections.emptyList();
        }
        return session.getQuestionIds();
    }

    public void finishSession(Long playerId) {
        log.warn("Clearing round results");
        quizSessions.remove(playerId);
    }
}

