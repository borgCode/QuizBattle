package org.borg.backend.achievement.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.model.*;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.shared.util.ImageUtil;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

import static org.borg.backend.achievement.events.AchievementEvents.*;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final PlayerRepository playerRepository;
    private final UserUnlockedAchievementRepository userUnlockedAchievementRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public List<UserUnlockedAchievementDTO> getUnlockedAchievements(Long playerId) {
        return null;
    }

    public void handleStoryAchievement(Long playerId, String storyName) {
        Achievement achievement = achievementRepository.findByName(storyName);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        if (userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement)) {
            return;
        }

        UserUnlockedAchievement unlockedAchievement = UserUnlockedAchievement.builder()
                .player(player)
                .achievement(achievement)
                .currentLevel(achievement.getLevels().get(0))
                .achievedAt(Instant.now())
                .build();

        userUnlockedAchievementRepository.save(unlockedAchievement);
        sendAchievementNotification(player, unlockedAchievement);
    }
    
    public void handleCategoryAchievement(Long playerId, String category) {
        Achievement achievement = achievementRepository.findByName(category);
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        int correctAnswers = player.getStats().getCategoryStats().get(category).getCorrect();

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, correctAnswers);
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
        
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVictoryAchievement(Long playerId) {
        Achievement achievement = achievementRepository.findByName("Victories");

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        int numOfWins = player.getStats().getNumOfWins();

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, numOfWins);
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
    }

    private AchievementLevel determineNewAchievementLevel(UserUnlockedAchievement unlockedAchievement, Achievement achievement, int currentProgress) {
        if (unlockedAchievement == null) {
            AchievementLevel firstLevel = achievement.getLevels().get(0);
            
            return firstLevel.getRequirementValue() <= currentProgress ? firstLevel : null;
        }

        int currentLevel = unlockedAchievement.getCurrentLevel().getLevel();
        if (currentLevel >= achievement.getLevels().size()) {
            return null;
        }
        return achievement.getLevels().stream()
                .filter(level -> level.getLevel() == currentLevel + 1)
                .filter(level -> currentProgress >= level.getRequirementValue())
                .findFirst()
                .orElse(null);
    }

    private void handleAchievementLevelUpdate(Player player, Achievement achievement, UserUnlockedAchievement unlockedAchievement, AchievementLevel newLevel) {
        if (newLevel == null) {
            return;
        }
        if (unlockedAchievement == null) {
            unlockedAchievement = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(newLevel)
                    .achievedAt(Instant.now())
                    .build();
        } else {
            unlockedAchievement.setCurrentLevel(newLevel);
            unlockedAchievement.setAchievedAt(Instant.now());
        }

        userUnlockedAchievementRepository.save(unlockedAchievement);

        sendAchievementNotification(player, unlockedAchievement);
    }


    private void sendAchievementNotification(Player player, UserUnlockedAchievement unlockedAchievement) {
        AchievementNotification achievementNotification = AchievementNotification.builder()
                .achievementName(unlockedAchievement.getAchievement().getName())
                .achievementDescription(unlockedAchievement.getCurrentLevel().getDescription())
                .base64Image(ImageUtil.encodeAchievementImageToBase64(unlockedAchievement.getCurrentLevel().getImageUrl()))
                .earnedAt(unlockedAchievement.getAchievedAt())
                .build();
        
        simpMessagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/achievements", achievementNotification);
    }
}
