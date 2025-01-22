package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.AchievementNotification;
import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.mapper.AchievementMapper;
import org.borg.backend.player.model.*;
import org.borg.backend.player.repository.AchievementLevelHistoryRepository;
import org.borg.backend.player.repository.AchievementProgressRepository;
import org.borg.backend.player.repository.AchievementRepository;
import org.borg.backend.player.repository.UserUnlockedAchievementRepository;
import org.borg.backend.shared.util.ImageUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserUnlockedAchievementRepository userUnlockedAchievementRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PlayerService playerService;
    private final AchievementMapper achievementMapper;
    private final AchievementLevelHistoryRepository historyRepository;
    private final AchievementProgressRepository achievementProgressRepository;

    public List<UserUnlockedAchievementDTO> getUnlockedAchievements(long playerId) {
        return achievementMapper.multipleToUnlockedAchievementDTO(userUnlockedAchievementRepository.findAllByPlayerId(playerId));
    }

    public void handleStoryAchievement(Long playerId, String storyName) {
        log.debug("Processing story achievement for player {}, story: {}", playerId, storyName);

        Achievement achievement = achievementRepository.findByName(storyName);
        Player player = playerService.getPlayerById(playerId);

        if (historyRepository.existsByPlayerAndAchievement(player, achievement)) {
            log.debug("Player {} already has story achievement for {}", playerId, storyName);
            return;
        }

        AchievementLevelHistory achievementLevelHistory = historyRepository.save(AchievementLevelHistory.builder()
                .player(player)
                .achievement(achievement)
                .achievedLevel(achievement.getLevels().get(0))
                .achievedAt(Instant.now())
                .build());
        
        //TODO clean up

//        if (userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement)) {
//            log.debug("Player {} already has story achievement for {}", playerId, storyName);
//            return;
//        }
//
//        UserUnlockedAchievement unlockedAchievement = UserUnlockedAchievement.builder()
//                .player(player)
//                .achievement(achievement)
//                .currentLevel(achievement.getLevels().get(0))
//                .achievedAt(Instant.now())
//                .build();
//
//        userUnlockedAchievementRepository.save(unlockedAchievement);
        log.info("Player {} unlocked new story achievement: {}", player.getUsername(), storyName);
        sendAchievementNotification(player, achievementLevelHistory);
    }

    @Transactional
    public void handleCategoryAchievement(Long playerId, String category) {
        log.debug("Processing category achievement for player {}, category: {}", playerId, category);

        Achievement achievement = achievementRepository.findByName(category);
        Player player = playerService.getPlayerById(playerId);
        
        if (historyRepository.existsByPlayerAndAchievementLevel(player, achievement.getLastLevel())) {
            log.debug("Player {} is already at maximum achievement level: {} for achievement {}", playerId, achievement.getLastLevel(), achievement.getName());
            return;
        }

        int correctAnswers = player.getStats().getCategoryStats().get(category).getCorrect();
        log.debug("Player {} has {} correct answers in category {}",
                player.getUsername(), correctAnswers, category);
        
        AchievementProgress progress = achievementProgressRepository.findByPlayerAndAchievement(player, achievement);
        if (progress != null) {
            progress.setCurrentProgress(correctAnswers);
            log.debug("Current achievement level for player {} in category {}: {}. Current progress={}, next level={}",
                    playerId, category, progress.getCurrentLevel().getLevel(), progress.getCurrentProgress(), progress.getNextLevelRequirement());
        } else {
            progress = achievementProgressRepository.save(
                    AchievementProgress.builder()
                            .achievement(achievement)
                            .player(player)
                            .currentLevel(achievement.getLevels().get(0))
                            .currentProgress(correctAnswers)
                            .nextLevelRequirement(achievement.getLevels().get(0).getRequirementValue()).build()
            );
            //TODO fix tests 
//            log.debug("Progress created for achievement: {} for player: {}", progress.getAchievement().getName(), player.getId());
        }
        
//        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository
//                .findByPlayerAndAchievement(player, achievement);

//        if (unlockedAchievement != null) {
//            log.debug("Current achievement level for player {} in category {}: {}",
//                    playerId, category, unlockedAchievement.getCurrentLevel().getLevel());
//        }
        
        boolean isEligibleForNextAchievement = progress.getCurrentProgress() >= progress.getNextLevelRequirement();
        
        if (isEligibleForNextAchievement) {
            handleAchievementCompletion(progress);
        } else {
            log.debug("Player {} not eligible for next achievement level", playerId);
        }
        
        achievementProgressRepository.save(progress);
//        
//        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, correctAnswers);
//        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
    }

    private void handleAchievementCompletion(AchievementProgress progress) {
        AchievementLevelHistory completedAchievement = historyRepository.save(AchievementLevelHistory.builder()
                .player(progress.getPlayer())
                .achievement(progress.getAchievement())
                .achievedLevel(progress.getCurrentLevel())
                .achievedAt(Instant.now())
                .build());
        
        AchievementLevel nextLevel = progress.getAchievement().getNextLevel(progress.getCurrentLevel());
        if (nextLevel != null) {
            progress.setCurrentLevel(nextLevel);
            progress.setNextLevelRequirement(nextLevel.getRequirementValue());
        } else {
            log.debug("Player {} reached max level for achievement {}", progress.getPlayer().getId(), progress.getAchievement().getName());
        }
        
        sendAchievementNotification(progress.getPlayer(), completedAchievement);
    }

    @Transactional
    public void handleVictoryAchievement(Long playerId) {
        log.debug("Processing victory achievement for player ID: {}", playerId);

        Achievement achievement = achievementRepository.findByName("Victories");
        Player player = playerService.getPlayerById(playerId);

        int numOfWins = player.getStats().getNumOfWins();
        log.debug("Player {} has {} total victories", player.getUsername(), numOfWins);

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, numOfWins);
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
    }

    private AchievementLevel determineNewAchievementLevel(UserUnlockedAchievement unlockedAchievement, Achievement achievement, int currentProgress) {
        log.debug("Determining new achievement level - Current progress: {}", currentProgress);
        if (unlockedAchievement == null) {
            AchievementLevel firstLevel = achievement.getLevels().get(0);
            log.debug("No current achievement, checking first level requirement: {}", firstLevel.getRequirementValue());

            return firstLevel.getRequirementValue() <= currentProgress ? firstLevel : null;
        }

        int currentLevel = unlockedAchievement.getCurrentLevel().getLevel();
        if (currentLevel >= achievement.getLevels().size()) {
            log.debug("Already at maximum achievement level: {}", currentLevel);
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
            log.debug("No new achievement level available for player {} in {}",
                    player.getUsername(), achievement.getName());
            return;
        }

        if (unlockedAchievement == null) {
            log.debug("Creating new achievement entry for player {} in {}",
                    player.getUsername(), achievement.getName());
            unlockedAchievement = UserUnlockedAchievement.builder()
                    .player(player)
                    .achievement(achievement)
                    .currentLevel(newLevel)
                    .achievedAt(Instant.now())
                    .build();
        } else {
            log.debug("Updating achievement level for player {} in {} from {} to {}",
                    player.getUsername(), achievement.getName(),
                    unlockedAchievement.getCurrentLevel().getLevel(), newLevel.getLevel());
            unlockedAchievement.setCurrentLevel(newLevel);
            unlockedAchievement.setAchievedAt(Instant.now());
        }

        userUnlockedAchievementRepository.save(unlockedAchievement);
        log.info("Player {} reached {} achievement level {}: {}",
                player.getUsername(), achievement.getName(),
                newLevel.getLevel(), newLevel.getDescription());

        sendAchievementNotification(player, unlockedAchievement);
    }

    private void sendAchievementNotification(Player player, UserUnlockedAchievement unlockedAchievement) {
        log.debug("Sending achievement notification to player {} for achievement {}",
                player.getUsername(), unlockedAchievement.getAchievement().getName());

        AchievementNotification achievementNotification = AchievementNotification.builder()
                .achievementName(unlockedAchievement.getAchievement().getName())
                .achievementDescription(unlockedAchievement.getCurrentLevel().getDescription())
                .base64Image(ImageUtil.encodeAchievementImageToBase64(unlockedAchievement.getCurrentLevel().getImageUrl()))
                .earnedAt(unlockedAchievement.getAchievedAt())
                .build();

        simpMessagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/achievements", achievementNotification);
        log.debug("Achievement notification sent successfully");
    }

    private void sendAchievementNotification(Player player, AchievementLevelHistory achievementLevelHistory) {
        log.debug("Sending achievement notification to player {} for achievement {}",
                player.getUsername(), achievementLevelHistory.getAchievement().getName());

        AchievementNotification achievementNotification = AchievementNotification.builder()
                .achievementName(achievementLevelHistory.getAchievement().getName())
                .achievementDescription(achievementLevelHistory.getAchievedLevel().getDescription())
                .base64Image(ImageUtil.encodeAchievementImageToBase64(achievementLevelHistory.getAchievedLevel().getImageUrl()))
                .earnedAt(achievementLevelHistory.getAchievedAt())
                .build();
        

        simpMessagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/achievements", achievementNotification);
        log.debug("Achievement notification sent successfully");
    }
}
