package org.borg.backend.unit.game.singleplayer;

import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterSession;
import org.borg.backend.game.singleplayer.service.ChapterSessionService;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ChapterSessionServiceTest {

    private ChapterSessionService chapterSessionService;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        chapterSessionService = new ChapterSessionService();
        chapter = Chapter.builder()
                .id(1L)
                .categories(Set.of("Category1", "Category2"))
                .roundWinCondition(2)
                .build();
    }

    @Test
    void shouldInitializeChapterSession() {
        chapterSessionService.initializeChapterSession(1L, chapter);

        ChapterSession session = chapterSessionService.getSession(1L);
        assertNotNull(session);
        assertEquals(1L, session.getChapterId());
        assertEquals(2, session.getWinCondition());
    }
    @Test
    void shouldProcessRoundResults_WhenRoundIsPassed() {
        chapterSessionService.initializeChapterSession(1L, chapter);

        List<Boolean> results = List.of(true, true, false, false, false);
        ChapterRoundResults roundResults = chapterSessionService.getRoundResults(1L, results);

        assertTrue(roundResults.isRoundPassed());
        assertFalse(roundResults.isGameOver());
        assertFalse(roundResults.isChapterComplete());
        assertEquals(3, roundResults.getCurrentHealth());
    }

    @Test
    void shouldProcessRoundResults_WhenRoundIsNotPassed() {
        chapterSessionService.initializeChapterSession(1L, chapter);

        List<Boolean> results = List.of(true, false, false, false, false);
        ChapterRoundResults roundResults = chapterSessionService.getRoundResults(1L, results);

        assertFalse(roundResults.isRoundPassed());
        assertFalse(roundResults.isGameOver());
        assertFalse(roundResults.isChapterComplete());
        assertEquals(2, roundResults.getCurrentHealth());
    }

    @Test
    void shouldClearSession() {
        chapterSessionService.initializeChapterSession(1L, chapter);
        chapterSessionService.clearSession(1L);
        
        assertThrows(ResourceNotFoundException.class, () -> chapterSessionService.getSession(1L));
    }
    
}
