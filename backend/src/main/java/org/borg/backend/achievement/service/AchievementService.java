package org.borg.backend.achievement.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.model.*;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.common.util.ImageUtil;
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

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

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
                .achievedAt(Instant.now())
                .build();

        log.warn("Saving {} to DB", unlockedAchievement.getAchievement().getName());

        userUnlockedAchievementRepository.save(unlockedAchievement);
        
        sendAchievementNotification(player, unlockedAchievement);

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
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
        
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleGameWonEvent(AchievementEvents.GameWonEvent event) {
        Achievement achievement = achievementRepository.findByName("Victories");

        Player player = playerRepository.findById(event.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        int numOfWins = player.getStats().getNumOfWins();

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, numOfWins);
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
    }

    private AchievementLevel determineNewAchievementLevel(UserUnlockedAchievement unlockedAchievement, Achievement achievement, int currentProgress) {
        if (unlockedAchievement == null) {
            AchievementLevel firstLevel = achievement.getLevels().get(0);
            log.warn("First level requirement: {}", firstLevel);
            
            return firstLevel.getRequirementValue() <= currentProgress ? firstLevel : null;
        }

        int currentLevel = unlockedAchievement.getCurrentLevel().getLevel();
        log.warn("Current level is: {}", currentLevel);
        log.warn("Achievement levels size: " + achievement.getLevels().size());
        if (currentLevel >= achievement.getLevels().size()) {
            log.warn("Returning null");
            return null;
        }
        
        log.warn("Checking if player is eligible for next level");
        
        return achievement.getLevels().stream()
                .filter(level -> level.getLevel() == currentLevel + 1)
                .filter(level -> currentProgress >= level.getRequirementValue())
                .findFirst()
                .orElse(null);
    }

    private void handleAchievementLevelUpdate(Player player, Achievement achievement, UserUnlockedAchievement unlockedAchievement, AchievementLevel newLevel) {
        if (newLevel == null) {
            log.warn("New level is null");
            return;
        }
        if (unlockedAchievement == null) {
            log.warn("Not unlocked, creating new achievement");
            unlockedAchievement = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(newLevel)
                    .achievedAt(Instant.now())
                    .build();
        } else {
            log.warn("Already unlocked, updating to new level is applicable, {}", newLevel);
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
        
        log.warn("Username is: {} ", player.getUsername());
        log.warn("Sending achievement notif");
        simpMessagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/achievements", achievementNotification);
    }
}
