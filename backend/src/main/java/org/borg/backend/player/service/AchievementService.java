package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.AchievementNotification;
import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.mapper.AchievementMapper;
import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.UserUnlockedAchievement;
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

    public List<UserUnlockedAchievementDTO> getUnlockedAchievements(long playerId) {
        return achievementMapper.multipleToUnlockedAchievementDTO(userUnlockedAchievementRepository.findAllByPlayerId(playerId));
    }

    public void handleStoryAchievement(Long playerId, String storyName) {
        Achievement achievement = achievementRepository.findByName(storyName);

        Player player = playerService.getPlayerById(playerId);

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

    @Transactional
    public void handleCategoryAchievement(Long playerId, String category) {
        
        Achievement achievement = achievementRepository.findByName(category);
        Player player = playerService.getPlayerById(playerId);

        int correctAnswers = player.getStats().getCategoryStats().get(category).getCorrect();

        UserUnlockedAchievement unlockedAchievement = userUnlockedAchievementRepository.findByPlayerAndAchievement(player, achievement);

        AchievementLevel newLevel = determineNewAchievementLevel(unlockedAchievement, achievement, correctAnswers);
        handleAchievementLevelUpdate(player, achievement, unlockedAchievement, newLevel);
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
}
