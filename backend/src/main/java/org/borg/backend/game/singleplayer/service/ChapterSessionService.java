package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChapterSessionService {
    private final Map<Long, ChapterSession> activeSessions = new ConcurrentHashMap<>();

    public void initializeChapterSession(Long playerId, Chapter chapter) {
        ChapterSession chapterSession = new ChapterSession(
                chapter.getId(),
                chapter.getCategories(),
                chapter.getRoundWinCondition());

        activeSessions.put(playerId, chapterSession);
    }

    public ChapterRoundResults getRoundResults(Long playerId, List<Boolean> results) {
        processRoundCompletion(playerId, results);

        ChapterSession session = activeSessions.get(playerId);

        return new ChapterRoundResults(results, session.isRoundPassed(), session.isGameOver(), session.isChapterComplete(), session.getCurrentHealth()
        );
    }

    public void processRoundCompletion(Long playerId, List<Boolean> roundResults) {
        ChapterSession currentSession = activeSessions.get(playerId);

        long correctAnswers = roundResults.stream()
                .filter(results -> results)
                .count();

        currentSession.setRoundPassed(correctAnswers >= currentSession.getWinCondition());

        if (correctAnswers < currentSession.getWinCondition()) {
            currentSession.decrementHealth();
        } else {
            currentSession.incrementRound();
        }
    }

    public ChapterSession getSession(Long playerId) {
        ChapterSession session = activeSessions.get(playerId);
        if (session == null) {
            throw new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, String.format("No active session found for player: %d", playerId));
        }
        return session;
    }

    public void clearSession(Long playerId) {
        activeSessions.remove(playerId);
    }
}
