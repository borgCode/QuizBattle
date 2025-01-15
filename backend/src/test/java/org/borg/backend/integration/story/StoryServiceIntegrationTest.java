package org.borg.backend.integration.story;

import Config.TestDataLoader;
import org.borg.backend.game.singleplayer.dto.AllStoriesDTO;
import org.borg.backend.game.singleplayer.service.StoryService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDataLoader.class)
public class StoryServiceIntegrationTest {
    
    @Autowired
    private PlayerProgressRepository playerProgressRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private TestDataLoader testDataLoader;

    Player testPlayer;
    @Autowired
    private StoryService storyService;

    @BeforeEach
    void setUp() {
        playerProgressRepository.deleteAll();
        playerRepository.deleteAll();
        testDataLoader.cleanup();
        
        String[] categories = {"Math", "Science", "History"};
        testDataLoader.createTestStories(3, categories);
        
        testPlayer = testDataLoader.createTestPlayer();
        
    }
    
    @Test
    void shouldCreateInitialProgressForAllStories_AndReturnAllStoriesDTO() {
        List<PlayerProgress> noProgressInitiatedList = playerProgressRepository.findAll();
        assertTrue(noProgressInitiatedList.isEmpty(), "Progress should be empty");
        
        AllStoriesDTO storiesDTO = storyService.getAllStories(testPlayer.getId());
        assertEquals(3, storiesDTO.getStories().size(), "There should be three storyDTOs returned");
        assertEquals(3, storiesDTO.getPlayerProgressList().size(), "There should be three progressDTOs returned");
        
        List<PlayerProgress> progressAfterGettingStories = playerProgressRepository.findAll();
        assertEquals(3, progressAfterGettingStories.size(), "Three player progress should be initiated");
    }
    @Test
    void shouldNewStoriesToPlayerProgress() {
        storyService.getAllStories(testPlayer.getId());

        String[] categories = {"Geography", "Movies"};
        testDataLoader.createTestStories(2, categories);

        AllStoriesDTO storiesDTO = storyService.getAllStories(testPlayer.getId());
        assertEquals(5, storiesDTO.getStories().size(), "There should be five stories returned");
        assertEquals(5, storiesDTO.getPlayerProgressList().size(), "There should be five progressDTOs returned");
    }
}
