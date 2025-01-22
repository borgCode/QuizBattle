package org.borg.backend.unit.player;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.AchievementNotification;
import org.borg.backend.player.model.*;
import org.borg.backend.player.repository.AchievementLevelHistoryRepository;
import org.borg.backend.player.repository.AchievementProgressRepository;
import org.borg.backend.player.repository.AchievementRepository;
import org.borg.backend.player.repository.UserUnlockedAchievementRepository;
import org.borg.backend.player.service.AchievementService;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.util.ImageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private UserUnlockedAchievementRepository userUnlockedAchievementRepository;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private AchievementLevelHistoryRepository historyRepository;
    @Mock
    private AchievementProgressRepository progressRepository;

    @InjectMocks
    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    class StoryAchievementTests {
        private final Long PLAYER_ID = 1L;
        private final String STORY_NAME = "Tutorial";
        private Player player;

        @BeforeEach
        void setUp() {
            player = Player.builder()
                    .id(PLAYER_ID)
                    .username("testuser123")
                    .build();
        }

        @Test
        void handleStoryAchievement_WhenEligible() {
            try (MockedStatic<ImageUtil> imageUtilMock = mockStatic(ImageUtil.class)) {

                AchievementLevel level1 = AchievementLevel.builder()
                        .level(1)
                        .description("Complete tutorial")
                        .imageUrl("/path/to/achievement.jpg")
                        .requirementValue(1)
                        .build();

                Achievement achievement = Achievement.builder()
                        .name(STORY_NAME)
                        .levels(List.of(level1))
                        .build();

                when(achievementRepository.findByName(STORY_NAME)).thenReturn(achievement);
                when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
                when(userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement))
                        .thenReturn(false);
                when(historyRepository.existsByPlayerAndAchievement(player, achievement)).thenReturn(false);
                
                when(historyRepository.save(any())).thenAnswer( invocationOnMock -> 
                        AchievementLevelHistory.builder()
                                .achievement(achievement)
                                .achievedLevel(achievement.getLevels().get(0))
                                .build());

                achievementService.handleStoryAchievement(PLAYER_ID, STORY_NAME);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

//                verifyAchievementSaved(player, achievement, level1);

                //TODO extract when all finished
                ArgumentCaptor<AchievementLevelHistory> captor = ArgumentCaptor.forClass(AchievementLevelHistory.class);
                verify(historyRepository).save(captor.capture());
                AchievementLevelHistory saved = captor.getValue();

                assertEquals(player, saved.getPlayer());
                assertEquals(achievement, saved.getAchievement());
                assertEquals(level1, saved.getAchievedLevel());

                verify(simpMessagingTemplate).convertAndSendToUser(
                        eq("testuser123"),
                        eq("/queue/achievements"),
                        any(AchievementNotification.class));
            }
        }

        @Test
        void handleStoryAchievementAlreadyUnlocked() {
            Achievement achievement = Achievement.builder()
                    .levels(List.of(AchievementLevel.builder().build()))
                    .name(STORY_NAME)
                    .build();

            when(achievementRepository.findByName(STORY_NAME)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(historyRepository.existsByPlayerAndAchievement(player, achievement)).thenReturn(true);
            
            achievementService.handleStoryAchievement(PLAYER_ID, STORY_NAME);

            verifyNoNotificationsSent();
        }
    }

    private void verifyAchievementSaved(Player expectedPlayer, Achievement expectedAchievement, AchievementLevel expectedLevel) {
        ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);
        verify(userUnlockedAchievementRepository).save(captor.capture());
        UserUnlockedAchievement saved = captor.getValue();

        assertEquals(expectedPlayer, saved.getPlayer());
        assertEquals(expectedAchievement, saved.getAchievement());
        assertEquals(expectedLevel, saved.getCurrentLevel());
    }

    @Nested
    class CategoryAchievementTests {
        private final Long PLAYER_ID = 1L;
        private final String CATEGORY = "Science & Nature";

        AchievementLevel levelOne;
        Player player;

        @BeforeEach
        void setUp() {
            player = Player.builder()
                    .id(PLAYER_ID)
                    .username("testuser123")
                    .stats(new Stats())
                    .build();

            levelOne = AchievementLevel.builder()
                    .level(1)
                    .description("Lab Assistant")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(5)
                    .build();
        }

        @Test
        void handleCategoryAchievement_EligibleForFirstLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(5);
            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));
            
            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne))
                    .build();
            
            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(progressRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(null);
            when(historyRepository.existsByPlayerAndAchievementLevel(player, levelOne)).thenReturn(false);
            
            when(progressRepository.save(any(AchievementProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(AchievementLevelHistory.class)))
                    .thenAnswer(invocation -> {
                        AchievementLevelHistory history = invocation.getArgument(0);
                        return AchievementLevelHistory.builder()
                                .player(history.getPlayer())
                                .achievement(history.getAchievement())
                                .achievedLevel(history.getAchievedLevel())
                                .achievedAt(history.getAchievedAt())
                                .build();
                    });
            
            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);
            
            ArgumentCaptor<AchievementProgress> progressCaptor = ArgumentCaptor.forClass(AchievementProgress.class);
            verify(progressRepository, times(2)).save(progressCaptor.capture());

            AchievementProgress savedProgress = progressCaptor.getValue();
            assertEquals(achievement, savedProgress.getAchievement());
            assertEquals(player, savedProgress.getPlayer());
            assertEquals(levelOne, savedProgress.getCurrentLevel());
            assertEquals(5, savedProgress.getCurrentProgress());
            assertEquals(5, savedProgress.getNextLevelRequirement());
            
            ArgumentCaptor<AchievementLevelHistory> historyCaptor = ArgumentCaptor.forClass(AchievementLevelHistory.class);
            verify(historyRepository).save(historyCaptor.capture());

            AchievementLevelHistory savedHistory = historyCaptor.getValue();
            assertEquals(player, savedHistory.getPlayer());
            assertEquals(achievement, savedHistory.getAchievement());
            assertEquals(levelOne, savedHistory.getAchievedLevel());

            verifyAchievementNotification("testuser123");
        }

        @Test
        void handleCategoryAchievement_EligibleForNextLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(10);
            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));
            
            AchievementLevel levelTwo = AchievementLevel.builder()
                    .level(2)
                    .description("Researcher")
                    .imageUrl("/path/to/achievement2.jpg")
                    .requirementValue(10)
                    .build();

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne, levelTwo))
                    .build();
            
            AchievementProgress existingProgress = AchievementProgress.builder()
                    .achievement(achievement)
                    .player(player)
                    .currentLevel(levelOne)
                    .currentProgress(10)
                    .nextLevelRequirement(10)
                    .build();
            
            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(progressRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(existingProgress);
            when(historyRepository.existsByPlayerAndAchievementLevel(player, levelTwo)).thenReturn(false);
            when(progressRepository.save(any(AchievementProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(AchievementLevelHistory.class)))
                    .thenAnswer(invocation -> {
                        AchievementLevelHistory history = invocation.getArgument(0);
                        return AchievementLevelHistory.builder()
                                .player(history.getPlayer())
                                .achievement(history.getAchievement())
                                .achievedLevel(history.getAchievedLevel())
                                .achievedAt(history.getAchievedAt())
                                .build();
                    });
            
            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);
            
            ArgumentCaptor<AchievementProgress> progressCaptor = ArgumentCaptor.forClass(AchievementProgress.class);
            verify(progressRepository).save(progressCaptor.capture());

            AchievementProgress savedProgress = progressCaptor.getValue();
            assertEquals(levelTwo, savedProgress.getCurrentLevel());
            assertEquals(10, savedProgress.getCurrentProgress());
            assertEquals(10, savedProgress.getNextLevelRequirement());
            
            verify(historyRepository).save(any(AchievementLevelHistory.class));

            verifyAchievementNotification("testuser123");
        }

        @Test
        void handleCategoryAchievement_NotEligibleForNextLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(4);
            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne))
                    .build();
            
            AchievementProgress existingProgress = AchievementProgress.builder()
                    .achievement(achievement)
                    .player(player)
                    .currentLevel(levelOne)
                    .currentProgress(4)
                    .nextLevelRequirement(5)
                    .build();

            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(progressRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(existingProgress);
            when(historyRepository.existsByPlayerAndAchievementLevel(player, levelOne)).thenReturn(false);
            when(progressRepository.save(any(AchievementProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);
            
            verify(progressRepository).save(any(AchievementProgress.class));
            verify(historyRepository, never()).save(any(AchievementLevelHistory.class));
           verifyNoNotificationsSent();
        }

        @Test
        void handleCategoryAchievement_AlreadyAtMaxLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(15);
            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

            AchievementLevel maxLevel = AchievementLevel.builder()
                    .level(3)
                    .description("Lab Master")
                    .imageUrl("/path/to/achievement3.jpg")
                    .requirementValue(15)
                    .build();

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne, maxLevel))
                    .build();

            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(historyRepository.existsByPlayerAndAchievementLevel(player, maxLevel)).thenReturn(true);

            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);
            
            verify(progressRepository, never()).save(any(AchievementProgress.class));
            verify(historyRepository, never()).save(any(AchievementLevelHistory.class));
            verifyNoNotificationsSent();
        }
    }

    @Nested
    class VictoryTests {
        private final Long PLAYER_ID = 1L;
        private final String CATEGORY = "Victories";

        AchievementLevel levelOne;
        Player player;

        @BeforeEach
        void setUp() {
            player = Player.builder()
                    .id(PLAYER_ID)
                    .username("testuser123")
                    .stats(new Stats())
                    .build();

            levelOne = AchievementLevel.builder()
                    .level(1)
                    .description("Victorious Beginner")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(5)
                    .build();
        }

        @Test
        void handleGameWon_EligibleForFirstLevel() {
            try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
                player.getStats().setNumOfWins(5);

                Achievement achievement = Achievement.builder()
                        .name(CATEGORY)
                        .levels(List.of(levelOne))
                        .build();

                when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
                when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
                when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(null);

                achievementService.handleVictoryAchievement(PLAYER_ID);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

                verifyAchievementSaved(player, achievement, levelOne);

                verifyAchievementNotification("testuser123");
            }
        }

        @Test
        void handleGameWon_EligibleForNextLevel() {
            try (MockedStatic<ImageUtil> imageUtilMock = mockStatic(ImageUtil.class)) {
                player.getStats().setNumOfWins(10);

                AchievementLevel level2 = AchievementLevel.builder()
                        .level(2)
                        .description("Victorious veteran")
                        .imageUrl("/path/to/achievement.jpg")
                        .requirementValue(10)
                        .build();

                Achievement achievement = Achievement.builder()
                        .name(CATEGORY)
                        .levels(List.of(levelOne, level2))
                        .build();

                UserUnlockedAchievement existingUnlock = UserUnlockedAchievement.builder()
                        .player(player)
                        .achievement(achievement)
                        .currentLevel(levelOne)
                        .build();

                when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
                when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
                when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(existingUnlock);

                achievementService.handleVictoryAchievement(PLAYER_ID);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

                verifyAchievementSaved(player, achievement, level2);

                verifyAchievementNotification("testuser123");
            }
        }

        @Test
        void handleGameWon_NotEligibleForNextLevel() {
            player.getStats().setNumOfWins(4);

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne))
                    .build();

            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(null);

            achievementService.handleVictoryAchievement(PLAYER_ID);

            verifyNoNotificationsSent();
        }

        @Test
        void handleGameWonAlreadyMaxLevel() {
            player.getStats().setNumOfWins(16);

            AchievementLevel levelTwo = AchievementLevel.builder()
                    .level(2)
                    .description("Victorious veteran")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(10)
                    .build();

            AchievementLevel levelThree = AchievementLevel.builder()
                    .level(3)
                    .description("Victories Master")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(15)
                    .build();

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne, levelTwo, levelThree))
                    .build();

            UserUnlockedAchievement existingUnlock = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(levelThree)
                    .build();

            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(existingUnlock);

            achievementService.handleVictoryAchievement(PLAYER_ID);

            verifyNoNotificationsSent();
        }
    }

    private void verifyAchievementNotification(String username) {
        verify(simpMessagingTemplate).convertAndSendToUser(
                eq(username),
                eq("/queue/achievements"),
                any(AchievementNotification.class));
    }

    private void verifyNoNotificationsSent() {
        verify(simpMessagingTemplate, never()).convertAndSendToUser(
                any(),
                any(),
                any()
        );
    }
}