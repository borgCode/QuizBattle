package org.borg.backend.game.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.shared.model.RoundAnswer;
import org.borg.backend.game.shared.model.RoundSession;
import org.borg.backend.game.shared.model.RoundType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RoundSessionService {
    private final ConcurrentHashMap<Long, RoundSession> roundSessions = new ConcurrentHashMap<>();

    public void initializeSession(Long playerId, List<Long> questionIds, String category, RoundType roundType) {
        RoundSession existingSession = roundSessions.get(playerId);

        if (existingSession != null) {
            if (!existingSession.isComplete()) {
                log.warn("Attempted to start new category while current category incomplete");
                return;
            }
            log.info("Round completed, removing session for player: {}", playerId);
            roundSessions.remove(playerId);
        }

        RoundSession session = new RoundSession(questionIds, category, roundType);
        roundSessions.put(playerId, session);
    }

    public boolean saveAnswer(Long playerId, Long questionId, boolean isCorrect) {
        RoundSession session = getActiveSession(playerId);

        if (session.hasAnsweredQuestion(questionId)) {
            log.warn("Attempted to answer same question twice: {}", questionId);
            throw new IllegalStateException("Question has already been answered");
        }

        RoundAnswer answer = new RoundAnswer(questionId, isCorrect, session.getCurrentIndex());
        session.getAnswers().put(session.getCurrentIndex(), answer);
        session.setCurrentIndex(session.getCurrentIndex() + 1);
        session.getAnsweredQuestionIds().add(questionId);

        return session.isComplete();
    }

    public boolean isQuestionAnswered(Long playerId, Long questionId) {
        RoundSession session = roundSessions.get(playerId);
        return session != null && session.hasAnsweredQuestion(questionId);
    }

    public String getCurrentCategory(Long playerId) {
        RoundSession session = roundSessions.get(playerId);
        return session != null ? session.getCategory() : null;
    }

    public List<Boolean> getSessionAnswers(Long playerId) {
        RoundSession session = roundSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active session found for player: " + playerId);
        }
        return session.getAnswers().values().stream()
                .sorted(java.util.Comparator.comparingInt(RoundAnswer::index))
                .map(RoundAnswer::correct)
                .toList();
    }

    public List<Long> getSessionQuestions(Long playerId) {
        RoundSession session = roundSessions.get(playerId);
        if (session == null) {
            return Collections.emptyList();
        }
        if (session.isComplete()) {
            roundSessions.remove(playerId);
            return Collections.emptyList();
        }
        return session.getQuestionIds();
    }

    public void finishSession(Long playerId) {
        log.info("Clearing round session for player: {}", playerId);
        roundSessions.remove(playerId);
    }

    private RoundSession getActiveSession(Long playerId) {
        RoundSession session = roundSessions.get(playerId);
        if (session == null) {
            throw new IllegalStateException("No active session found for player: " + playerId);
        }
        return session;
    }
}
