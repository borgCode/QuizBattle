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
    UserUnlockedAchievementDTO toUnlockedAchievementDTO(UserUnlockedAchievement unlockedAchievement);

    List<UserUnlockedAchievementDTO> multipleToUnlockedAchievementDTO(List<UserUnlockedAchievement> userUnlockedAchievements);
    
}
