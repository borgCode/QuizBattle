package org.borg.backend.game.singleplayer.service;

import lombok.Data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ChapterSession {
    private final Long chapterId;
    private final List<String> categories;
    private final int winCondition;
    private int currentRound = 0;
    private int currentHealth = 3;
    private boolean isRoundPassed;
    private final Set<Long> completeRoundIds = ConcurrentHashMap.newKeySet();

    public ChapterSession(Long chapterId, Set<String> categories, int winCondition) {
        this.chapterId = chapterId;
        this.categories = new ArrayList<>(categories);
        this.winCondition = winCondition;
    }
    
    public void decrementHealth() {
        this.currentHealth--;
    }
    
    public void incrementRound() {
        this.currentRound++;
    }

    public boolean isGameOver() {
        return currentHealth <= 0;
    }

    public boolean isChapterComplete() {
        return currentRound >= categories.size();
    }

    public String getCurrentCategory() {
        if (isChapterComplete()) {
            throw new IllegalStateException("Chapter is already complete");
        }
        return categories.get(currentRound);
    }
}
