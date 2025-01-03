package org.borg.backend.achievement.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.model.AchievementLevel;
import org.borg.backend.achievement.model.AchievementNotification;
import org.borg.backend.achievement.model.UserUnlockedAchievement;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.common.util.ImageUtil;
import org.borg.backend.player.model.CategoryStats;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private PlayerRepository playerRepository;
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


    @Test
    void handleStoryAchievementNotUnlocked() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {

            String storyName = "Tutorial";
            Long playerId = 1L;

            Player player = Player.builder()
                    .id(playerId)
                    .username("testuser123")
                    .build();

            AchievementLevel level1 = AchievementLevel.builder()
                    .level(1)
                    .description("Complete tutorial")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(1)
                    .build();

            Achievement achievement = Achievement.builder()
                    .name(storyName)
                    .levels(List.of(level1))
                    .build();


            when(achievementRepository.findByName(storyName)).thenReturn(achievement);
            when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
            when(userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement))
                    .thenReturn(false);

            achievementService.handleStoryCompleted(new AchievementEvents.StoryCompletedEvent(playerId, storyName));

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
        String storyName = "Tutorial";
        Long playerId = 1L;

        Player player = Player.builder()
                .id(playerId)
                .username("testuser123")
                .build();

        Achievement achievement = Achievement.builder()
                .name(storyName)
                .build();

        when(achievementRepository.findByName(storyName)).thenReturn(achievement);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement))
                .thenReturn(true);


        achievementService.handleStoryCompleted(new AchievementEvents.StoryCompletedEvent(playerId, storyName));

        verify(userUnlockedAchievementRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSendToUser(
                any(),
                any(),
                any()
        );

    }

    @Test
    void handleCategoryCompletedAndEligibleForNextLevel() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            String category = "Science & Nature";
            Long playerId = 1L;

            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setCorrect(10);

            Stats stats = new Stats();
            stats.setCategoryStats(Map.of(category, categoryStats));

            Player player = Player.builder()
                    .id(playerId)
                    .username("testuser123")
                    .stats(stats)
                    .build();
            
            AchievementLevel level1 = AchievementLevel.builder()
                    .level(1)
                    .description("Lab Assistant")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(5)
                    .build();

            AchievementLevel level2 = AchievementLevel.builder()
                    .level(2)
                    .description("Researcher")
                    .imageUrl("/path/to/achievement.jpg")
                    .requirementValue(10)
                    .build();


            Achievement achievement = Achievement.builder()
                    .name(category)
                    .levels(List.of(level1, level2))
                    .build();

            UserUnlockedAchievement existingUnlock = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(level1)
                    .build();
            
            when(achievementRepository.findByName(category)).thenReturn(achievement);
            when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
            when(userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement)).thenReturn(existingUnlock);
            
            achievementService.handleCategoryCompletedEvent(new AchievementEvents.CategoryCompletedEvent(playerId, category));
            
            imageUtilMock.when(() -> ImageUtil.encodeAchievementImageToBase64("/path/to/achievement.jpg"))
                    .thenReturn("base64Image");
            
            ArgumentCaptor<UserUnlockedAchievement> captor = ArgumentCaptor.forClass(UserUnlockedAchievement.class);

            verify(userUnlockedAchievementRepository).save(captor.capture());
            UserUnlockedAchievement newUnlockedAchievement = captor.getValue();
            
            assertEquals(player, newUnlockedAchievement.getPlayer());
            assertEquals(level2, newUnlockedAchievement.getCurrentLevel());
            assertEquals(achievement, newUnlockedAchievement.getAchievement());

            verify(simpMessagingTemplate).convertAndSendToUser(
                    eq("testuser123"),
                    eq("/queue/achievements"),
                    any(AchievementNotification.class));
            
            
        }
    }

    @Test
    void handleGameWonEvent() {
    }
}