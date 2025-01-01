package org.borg.backend.achievement.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.model.AchievementLevel;
import org.borg.backend.achievement.model.UserUnlockedAchievement;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final PlayerRepository playerRepository;
    private final UserUnlockedAchievementRepository userUnlockedAchievementRepository;

    @EventListener
    @Async
    public void handleStoryCompleted(AchievementEvents.StoryCompletedEvent event) {
        log.warn("Handling story completed");

        Achievement achievement = achievementRepository.findByName(event.storyName());

        Player player = playerRepository.findById(event.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        if (userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement)) {
            //TODO error handling for already exists
            return;
        }

        UserUnlockedAchievement unlockedAchievement = UserUnlockedAchievement.builder()
                .player(player)
                .achievement(achievement)
                .currentLevel(achievement.getLevels().get(0))
                .achievedAt(LocalDateTime.now())
                .build();

        log.warn("Saving {} to DB", unlockedAchievement.getAchievement().getName());

        userUnlockedAchievementRepository.save(unlockedAchievement);

    }

    @EventListener
    @Async
    public void handleCategoryCompletedEvent(AchievementEvents.CategoryCompletedEvent event) {

        Achievement achievement = achievementRepository.findByName(event.category());

        Player player = playerRepository.findById(event.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        int correctAnswers = player.getStats().getCategoryStats().get(event.category()).getCorrect();

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, correctAnswers);
        
        if (newLevel == null) {
            return;
        }
        
        if (unlockedAchievement == null) {
            unlockedAchievement = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(newLevel)
                    .achievedAt(LocalDateTime.now())
                    .build();
        } else {
            unlockedAchievement.setCurrentLevel(newLevel);
            unlockedAchievement.setAchievedAt(LocalDateTime.now());
        }
        
        userUnlockedAchievementRepository.save(unlockedAchievement);
    }

    private AchievementLevel determineNewAchievementLevel(UserUnlockedAchievement unlockedAchievement, Achievement achievement, int correctAnswers) {
        if (unlockedAchievement == null) {
            AchievementLevel firstLevel = achievement.getLevels().get(0);
            return firstLevel.getRequirementValue() <= correctAnswers ? firstLevel : null;
        }

        int currentLevel = unlockedAchievement.getCurrentLevel().getLevel();
        if (currentLevel >= achievement.getLevels().size() - 1) {
            return null;
        }
        
        return achievement.getLevels().stream()
                .filter(level -> level.getLevel() == currentLevel + 1)
                .filter(level -> correctAnswers >= level.getRequirementValue())
                .findFirst()
                .orElse(null);
    }
    
}
