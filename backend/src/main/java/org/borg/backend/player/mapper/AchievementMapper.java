package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.UserUnlockedAchievement;
import org.borg.backend.shared.util.ImageUtil;

import java.util.Collections;
import java.util.List;

public class AchievementMapper {
    public static UserUnlockedAchievementDTO toUnlockedAchievementDTO(UserUnlockedAchievement unlockedAchievement) {
        if (unlockedAchievement == null) {
            return null;
        }

        Achievement achievement = unlockedAchievement.getAchievement();
        AchievementLevel currentLevel = unlockedAchievement.getCurrentLevel();

        return UserUnlockedAchievementDTO.builder()
                .achievementName(achievement.getName())
                .achievementLevelName(currentLevel.getName())
                .description(currentLevel.getDescription())
                .imageUrl(ImageUtil.encodeAchievementImageToBase64(currentLevel.getImageUrl()))
                .nextLevelRequirement(determineNextLevelRequirement(achievement, currentLevel))
                .isMaxLevel(isMaxLevel(achievement, currentLevel))
                .build();
    }

    public static List<UserUnlockedAchievementDTO> multipleToUnlockedAchievementDTO(List<UserUnlockedAchievement> userUnlockedAchievements) {
        if (userUnlockedAchievements == null) {
            return List.of();
        }
        
        return userUnlockedAchievements.stream()
                .map(AchievementMapper::toUnlockedAchievementDTO)
                .toList();
    }
    
    private static int determineNextLevelRequirement(Achievement achievement, AchievementLevel currentLevel) {
        if (isMaxLevel(achievement, currentLevel)) {
            return currentLevel.getRequirementValue();
        }

        List<AchievementLevel> levels = achievement.getLevels();
        int currentLevelIndex = levels.indexOf(currentLevel);

        if (currentLevelIndex == -1 || currentLevelIndex + 1 >= levels.size()) {
            throw new IllegalStateException("Invalid achievement level configuration");
        }

        return levels.get(currentLevelIndex + 1).getRequirementValue();
    }

    private static boolean isMaxLevel(Achievement achievement, AchievementLevel currentLevel) {
        return achievement.getLevels().getLast().equals(currentLevel);
    }
}
