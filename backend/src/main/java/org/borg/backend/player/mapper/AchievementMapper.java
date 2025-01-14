package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.UserUnlockedAchievementDTO;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.UserUnlockedAchievement;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface AchievementMapper {

    @Mapping(target = "achievementName", source = "unlockedAchievement.achievement.name")
    @Mapping(target = "achievementLevelName", source = "unlockedAchievement.currentLevel.name")
    @Mapping(target = "description", source = "unlockedAchievement.currentLevel.description")
    @Mapping(target = "imageUrl", expression = "java(ImageUtil.encodeAchievementImageToBase64(unlockedAchievement.getCurrentLevel().getImageUrl()))")
    @Mapping(target = "nextLevelRequirement", expression = "java(determineNextLevelRequirement(unlockedAchievement.getAchievement().getLevels(), unlockedAchievement.getCurrentLevel()))")
    @Mapping(target = "isMaxLevel", expression = "java(isMaxLevel(unlockedAchievement.getAchievement().getLevels(), unlockedAchievement.getCurrentLevel()))")
    UserUnlockedAchievementDTO toUnlockedAchievementDTO(UserUnlockedAchievement unlockedAchievement);

    List<UserUnlockedAchievementDTO> multipleToUnlockedAchievementDTO(List<UserUnlockedAchievement> userUnlockedAchievements);

    default int determineNextLevelRequirement(List<AchievementLevel> levels, AchievementLevel currentLevel) {
        if (isMaxLevel(levels, currentLevel)) {
            return currentLevel.getRequirementValue();
        }

        int currentLevelIndex = levels.indexOf(currentLevel);

        if (currentLevelIndex == -1 || currentLevelIndex + 1 >= levels.size()) {
            throw new IllegalStateException("Invalid achievement level configuration");
        }

        return levels.get(currentLevelIndex + 1).getRequirementValue();
    }

    default boolean isMaxLevel(List<AchievementLevel> levels, AchievementLevel currentLevel) {
        return levels.get(levels.size() - 1).equals(currentLevel);
    }
}
