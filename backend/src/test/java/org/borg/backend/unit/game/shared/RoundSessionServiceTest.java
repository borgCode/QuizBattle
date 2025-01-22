package org.borg.backend.unit.game.shared;

import org.borg.backend.game.shared.mapper.QuestionMapper;
import org.borg.backend.game.shared.model.RoundType;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoundSessionServiceTest {
    
    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private RoundSessionService roundSessionService = new RoundSessionService(questionRepository, questionMapper);

    private static final Long PLAYER_ID = 1L;
    private static final String CATEGORY = "Science";
    private static final List<Long> QUESTION_IDS = List.of(1L, 2L, 3L);

    @Test
    void initializeSession_WhenNoExistingSession_ShouldCreateNew() {
        assertNull(roundSessionService.getCurrentCategory(PLAYER_ID));
        
        roundSessionService.initializeSession(PLAYER_ID, QUESTION_IDS, CATEGORY, RoundType.MULTIPLAYER);
        
        assertEquals(CATEGORY, roundSessionService.getCurrentCategory(PLAYER_ID));
        assertEquals(QUESTION_IDS, roundSessionService.getSessionQuestions(PLAYER_ID));
    }
    
    @Test
    void saveAnswer_ShouldTrackProgress() {
        roundSessionService.initializeSession(PLAYER_ID, QUESTION_IDS, CATEGORY, RoundType.MULTIPLAYER);

        boolean isComplete = roundSessionService.saveAnswer(PLAYER_ID, QUESTION_IDS.get(0), true);

        assertFalse(isComplete);
        assertTrue(roundSessionService.isQuestionAnswered(PLAYER_ID, QUESTION_IDS.get(0)));
        assertEquals(List.of(true), roundSessionService.getSessionAnswers(PLAYER_ID));
    }
    
    @Test
    void saveAnswer_WhenAllQuestionsAnswered_ShouldMarkComplete() {
        roundSessionService.initializeSession(PLAYER_ID, List.of(1L), CATEGORY, RoundType.MULTIPLAYER);
        
        roundSessionService.saveAnswer(PLAYER_ID, 1L, true);
        roundSessionService.saveAnswer(PLAYER_ID, 2L, true);
        boolean isComplete = roundSessionService.saveAnswer(PLAYER_ID, 3L, true);
        
        assertTrue(isComplete);
    }

}
