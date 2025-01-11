package org.borg.backend.game.singleplayer.service;


import lombok.RequiredArgsConstructor;
import org.borg.backend.chapter.model.Chapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChapterSessionService {
    private final Map<Long, ChapterSession> activeSessions = new ConcurrentHashMap<>();
    
    public void initializeChapterSession(Long playerId, Set<String> categories, Chapter chapter) {
        ChapterSession chapterSession = new ChapterSession(
                chapter.getId(),
                categories,
                chapter.getRoundWinCondition());
        
        activeSessions.put(playerId, chapterSession);
    }
    
    
    
    public void processRoundCompletion(Long playerId, List<Boolean> roundResults) {
        ChapterSession currentSession = activeSessions.get(playerId);
        
        long correctAnswers = roundResults.stream()
                .filter(results -> results)
                .count();
        
        if (correctAnswers <= 2) {
            currentSession.decrementHealth();
        } else {
            currentSession.incrementRound();
        }
    }
    
}
