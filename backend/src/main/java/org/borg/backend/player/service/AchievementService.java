package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.AchievementDTO;
import org.borg.backend.player.dto.AchievementNotification;
import org.borg.backend.player.mapper.AchievementMapper;
import org.borg.backend.player.model.*;
import org.borg.backend.player.repository.AchievementLevelHistoryRepository;
import org.borg.backend.player.repository.AchievementProgressRepository;
import org.borg.backend.player.repository.AchievementRepository;
import org.borg.backend.shared.util.ImageUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PlayerService playerService;
    private final AchievementMapper achievementMapper;
    private final AchievementLevelHistoryRepository historyRepository;
    private final AchievementProgressRepository achievementProgressRepository;

    public List<AchievementDTO> getUnlockedAchievements(long playerId) {
        log.info("Fetching unlocked achievements for playerId: {}", playerId);

        List<AchievementProgress> achievementProgress = achievementProgressRepository.findByPlayerId(playerId);
        List<AchievementLevelHistory> levelHistory = historyRepository.findByPlayerId(playerId);

        if (achievementProgress.isEmpty()) {
            log.info("No achievements found for playerId: {}", playerId);
            return new ArrayList<>();
        }
        
        

        List<Achievement> achievements = achievementProgress.stream()
                .map(AchievementProgress::getAchievement)
                .collect(Collectors.toList());

        achievements.forEach(a -> log.debug("Achievement {} has levels: {}",
                a.getId(),
                a.getLevels().stream().map(AchievementLevel::getName).collect(Collectors.joining(", "))
        ));

        List<AchievementDTO> results = achievementMapper.multipleToDto(
                achievements,
                achievementProgress,
                levelHistory
        );
        
        results.forEach(dto -> {
            log.debug("Achievement {} has {} unlocked levels",
                    dto.getName(),
                    dto.getUnlockedLevels() != null ? dto.getUnlockedLevels().size() : 0);
        });

        return results;
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
                .achievementLevel(achievement.getLevels().get(0))
                .achievedAt(Instant.now())
                .build());
        
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
         
            log.debug("Progress created for achievement: {} for player: {}", progress.getAchievement().getName(), player.getId());
        }

        boolean isEligibleForNextAchievement = progress.getCurrentProgress() >= progress.getNextLevelRequirement();
        
        if (isEligibleForNextAchievement) {
            handleAchievementCompletion(progress);
        } else {
            log.debug("Player {} not eligible for next achievement level", playerId);
        }
        
        achievementProgressRepository.save(progress);
    }

    private void handleAchievementCompletion(AchievementProgress progress) {
        AchievementLevelHistory completedAchievement = historyRepository.save(AchievementLevelHistory.builder()
                .player(progress.getPlayer())
                .achievement(progress.getAchievement())
                .achievementLevel(progress.getCurrentLevel())
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

        if (historyRepository.existsByPlayerAndAchievementLevel(player, achievement.getLastLevel())) {
            log.debug("Player {} is already at maximum achievement level: {} for achievement {}", playerId, achievement.getLastLevel(), achievement.getName());
            return;
        }

        int numOfWins = player.getStats().getNumOfWins();
        log.debug("Player {} has {} total victories", player.getUsername(), numOfWins);

        AchievementProgress progress = achievementProgressRepository.findByPlayerAndAchievement(player, achievement);
        if (progress != null) {
            progress.setCurrentProgress(numOfWins);
            log.debug("Current achievement level for player {} in victories: {}. Current progress={}, next level={}",
                    playerId, progress.getCurrentLevel().getLevel(), progress.getCurrentProgress(), progress.getNextLevelRequirement());
        } else {
            progress = achievementProgressRepository.save(
                    AchievementProgress.builder()
                            .achievement(achievement)
                            .player(player)
                            .currentLevel(achievement.getLevels().get(0))
                            .currentProgress(numOfWins)
                            .nextLevelRequirement(achievement.getLevels().get(0).getRequirementValue()).build()
            );

            log.debug("Progress created for achievement: {} for player: {}", progress.getAchievement().getName(), player.getId());
        }

        boolean isEligibleForNextAchievement = progress.getCurrentProgress() >= progress.getNextLevelRequirement();

        if (isEligibleForNextAchievement) {
            handleAchievementCompletion(progress);
        } else {
            log.debug("Player {} not eligible for next achievement level", playerId);
        }

        achievementProgressRepository.save(progress);
    }
    
    private void sendAchievementNotification(Player player, AchievementLevelHistory achievementLevelHistory) {
        log.debug("Sending achievement notification to player {} for achievement {}",
                player.getUsername(), achievementLevelHistory.getAchievement().getName());

        AchievementNotification achievementNotification = AchievementNotification.builder()
                .achievementName(achievementLevelHistory.getAchievement().getName())
                .achievementDescription(achievementLevelHistory.getAchievementLevel().getDescription())
                .base64Image(ImageUtil.encodeAchievementImageToBase64(achievementLevelHistory.getAchievementLevel().getImageUrl()))
                .earnedAt(achievementLevelHistory.getAchievedAt())
                .build();
        

        simpMessagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/achievements", achievementNotification);
        log.debug("Achievement notification sent successfully");
    }
}
