package org.borg.backend.unit.player;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.AchievementNotification;
import org.borg.backend.player.model.*;
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
        void handleStoryAchievementNotUnlocked() {
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

                achievementService.handleStoryAchievement(PLAYER_ID, STORY_NAME);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

                ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);
                verify(userUnlockedAchievementRepository).save(captor.capture());

                UserUnlockedAchievement savedAchievement = captor.getValue();
                assertEquals(player, savedAchievement.getPlayer());
                assertEquals(achievement, savedAchievement.getAchievement());
                assertEquals(level1, savedAchievement.getCurrentLevel());

                verify(simpMessagingTemplate).convertAndSendToUser(
                        eq("testuser123"),
                        eq("/queue/achievements"),
                        any(AchievementNotification.class));
            }
        }

        @Test
        void handleStoryAchievementAlreadyUnlocked() {

            Achievement achievement = Achievement.builder()
                    .name(STORY_NAME)
                    .build();

            when(achievementRepository.findByName(STORY_NAME)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement))
                    .thenReturn(true);

            achievementService.handleStoryAchievement(PLAYER_ID, STORY_NAME);

            verifyNoNotificationsSent();
        }
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
        void handleCategoryCompleted_EligibleForFirstLevel() {
            try (MockedStatic<ImageUtil> imageUtilMock = mockStatic(ImageUtil.class)) {
                CategoryStats categoryStats = new CategoryStats();
                categoryStats.setCorrect(5);

                player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

                Achievement achievement = Achievement.builder()
                        .name(CATEGORY)
                        .levels(List.of(levelOne))
                        .build();

                when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
                when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
                when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(null);

                achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

                ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);

                verify(userUnlockedAchievementRepository).save(captor.capture());
                UserUnlockedAchievement newUnlockedAchievement = captor.getValue();

                assertEquals(player, newUnlockedAchievement.getPlayer());
                assertEquals(levelOne, newUnlockedAchievement.getCurrentLevel());
                assertEquals(achievement, newUnlockedAchievement.getAchievement());

                verifyAchievementNotification("testuser123");
            }
        }

        @Test
        void handleCategoryCompleted_EligibleForNextLevel() {
            try (MockedStatic<ImageUtil> imageUtilMock = mockStatic(ImageUtil.class)) {
                CategoryStats categoryStats = new CategoryStats();
                categoryStats.setCorrect(10);

                player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

                AchievementLevel level2 = AchievementLevel.builder()
                        .level(2)
                        .description("Researcher")
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

                achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);

                imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                        .thenReturn("base64Image");

                ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);

                verify(userUnlockedAchievementRepository).save(captor.capture());
                UserUnlockedAchievement newUnlockedAchievement = captor.getValue();

                assertEquals(player, newUnlockedAchievement.getPlayer());
                assertEquals(level2, newUnlockedAchievement.getCurrentLevel());
                assertEquals(achievement, newUnlockedAchievement.getAchievement());

                verifyAchievementNotification("testuser123");
            }
        }

        @Test
        void handleCategoryCompleted_NotEligibleForNextLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(4);

            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

            Achievement achievement = Achievement.builder()
                    .name(CATEGORY)
                    .levels(List.of(levelOne))
                    .build();

            when(achievementRepository.findByName(CATEGORY)).thenReturn(achievement);
            when(playerService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(null);

            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);

            verifyNoNotificationsSent();
        }

        @Test
        void handleCategoryCompleted_AlreadyMaxLevel() {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(16);

            player.getStats().setCategoryStats(Map.of(CATEGORY, categoryStats));

            AchievementLevel levelTwo = AchievementLevel.builder()
                    .level(2)
                    .description("Lab Cat")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(10)
                    .build();

            AchievementLevel levelThree = AchievementLevel.builder()
                    .level(3)
                    .description("Lab Dog")
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

            achievementService.handleCategoryAchievement(PLAYER_ID, CATEGORY);

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

                ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);

                verify(userUnlockedAchievementRepository).save(captor.capture());
                UserUnlockedAchievement newUnlockedAchievement = captor.getValue();

                assertEquals(player, newUnlockedAchievement.getPlayer());
                assertEquals(levelOne, newUnlockedAchievement.getCurrentLevel());
                assertEquals(achievement, newUnlockedAchievement.getAchievement());

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

                ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);

                verify(userUnlockedAchievementRepository).save(captor.capture());
                UserUnlockedAchievement newUnlockedAchievement = captor.getValue();

                assertEquals(player, newUnlockedAchievement.getPlayer());
                assertEquals(level2, newUnlockedAchievement.getCurrentLevel());
                assertEquals(achievement, newUnlockedAchievement.getAchievement());

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
        verify(userUnlockedAchievementRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSendToUser(
                any(),
                any(),
                any()
        );
    }
}