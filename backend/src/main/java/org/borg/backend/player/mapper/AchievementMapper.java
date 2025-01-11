package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.UserUnlockedAchievement;
import org.borg.backend.shared.util.ImageUtil;

import java.util.List;

public class AchievementMapper {
    public static UserUnlockedAchievementDTO toUnlockedAchievementDTO(UserUnlockedAchievement unlockedAchievement) {
        if (unlockedAchievement == null) {
            return null;
        }

        Achievement achievement = unlockedAchievement.getAchievement();
        AchievementLevel currentLevel = unlockedAchievement.getCurrentLevel();
        List<AchievementLevel> levels = achievement.getLevels();

        return UserUnlockedAchievementDTO.builder()
                .achievementName(achievement.getName())
                .achievementLevelName(currentLevel.getName())
                .description(currentLevel.getDescription())
                .imageUrl(ImageUtil.encodeAchievementImageToBase64(currentLevel.getImageUrl()))
                .nextLevelRequirement(determineNextLevelRequirement(levels, currentLevel))
                .isMaxLevel(isMaxLevel(levels, currentLevel))
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
    
    private static int determineNextLevelRequirement(List<AchievementLevel> levels, AchievementLevel currentLevel) {
        if (isMaxLevel(levels, currentLevel)) {
            return currentLevel.getRequirementValue();
        }
        
        int currentLevelIndex = levels.indexOf(currentLevel);

        if (currentLevelIndex == -1 || currentLevelIndex + 1 >= levels.size()) {
            throw new IllegalStateException("Invalid achievement level configuration");
        }

        return levels.get(currentLevelIndex + 1).getRequirementValue();
    }

    private static boolean isMaxLevel(List<AchievementLevel> levels, AchievementLevel currentLevel) {
        return levels.get(levels.size() - 1).equals(currentLevel);
    }
}
