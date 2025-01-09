package org.borg.backend.game.shared.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class RoundSession {
    private final List<Long> questionIds;
    private final String category;
    private final RoundType roundType;
    private final int maxQuestions;
    private final ConcurrentHashMap<Integer, RoundAnswer> answers;
    private final Set<Long> answeredQuestionIds;
    private final Instant timestamp;
    private int currentIndex;

    public RoundSession(List<Long> questionIds, String category, RoundType roundType) {
        this.questionIds = questionIds;
        this.category = category;
        this.roundType = roundType;
        this.maxQuestions = roundType == RoundType.SINGLE_PLAYER ? 5 : 3;
        this.answers = new ConcurrentHashMap<>();
        this.answeredQuestionIds = ConcurrentHashMap.newKeySet();
        this.timestamp = Instant.now();
        this.currentIndex = 0;
    }

    public boolean isComplete() {
        return answers.size() >= maxQuestions;
    }

    public boolean hasAnsweredQuestion(Long questionId) {
        return answeredQuestionIds.contains(questionId);
    }
}
