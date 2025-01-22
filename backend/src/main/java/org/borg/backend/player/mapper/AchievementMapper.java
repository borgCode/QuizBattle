package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.AchievementDTO;
import org.borg.backend.player.dto.AchievementLevelDTO;
import org.borg.backend.player.dto.AchievementProgressDTO;
import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.AchievementLevelHistory;
import org.borg.backend.player.model.AchievementProgress;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface AchievementMapper {
    @Mapping(target = "unlockedLevels", expression = "java(mapLevelHistory(levelHistory))")
    @Mapping(target = "totalLevels", expression = "java(achievement.getLevels().size())")
    @Mapping(target = "nextLevel", expression = "java(getNextLevel(achievement, progress))")
    @Mapping(target = "progress", source = "progress", qualifiedByName = "progress")
    @Mapping(target = "name", source = "achievement.name")
    AchievementDTO toDto(Achievement achievement, AchievementProgress progress,
                         List<AchievementLevelHistory> levelHistory);

    @Mapping(target = "name", source = "achievementLevel.name")
    @Mapping(target = "description", source = "achievementLevel.description")
    @Mapping(target = "imageUrl", expression = "java(ImageUtil.encodeAchievementImageToBase64(levelHistory.getAchievementLevel().getImageUrl()))")
    AchievementLevelDTO toLevelDto(AchievementLevelHistory levelHistory);

    @Mapping(target = "imageUrl", expression = "java(ImageUtil.encodeAchievementImageToBase64(level.getImageUrl()))")
    @Mapping(target = "achievedAt", ignore = true)
    AchievementLevelDTO toLevelDto(AchievementLevel level);

    default List<AchievementLevelDTO> mapLevelHistory(List<AchievementLevelHistory> levelHistory) {
        return levelHistory == null ? Collections.emptyList()
                : levelHistory.stream()
                .map(this::toLevelDto)
                .collect(Collectors.toList());
    }

    @Named("nextLevel")
    default AchievementLevelDTO getNextLevel(Achievement achievement, AchievementProgress progress) {
        if (progress == null || progress.getCurrentLevel() == null) {
            return achievement.getLevels().isEmpty() ? null
                    : toLevelDto(achievement.getLevels().get(0));
        }
        return toLevelDto(achievement.getNextLevel(progress.getCurrentLevel()));
    }

    @Named("progress")
    default AchievementProgressDTO toProgressDto(AchievementProgress progress) {
        if (progress == null) {
            return AchievementProgressDTO.builder()
                    .currentProgress(0)
                    .requirementForNext(0)
                    .progressPercentage(0.0)
                    .build();
        }

        double percentage = progress.getNextLevelRequirement() > 0
                ? (progress.getCurrentProgress() * 100.0) / progress.getNextLevelRequirement()
                : 0.0;

        return AchievementProgressDTO.builder()
                .currentProgress(progress.getCurrentProgress())
                .requirementForNext(progress.getNextLevelRequirement())
                .progressPercentage(percentage)
                .build();
    }

    default List<AchievementDTO> multipleToDto(List<Achievement> achievements,
                                               List<AchievementProgress> progress,
                                               List<AchievementLevelHistory> levelHistory) {
        if (achievements == null || progress == null) {
            return Collections.emptyList();
        }
        
        Map<Long, List<AchievementLevelHistory>> historyMap;
        if (levelHistory == null) {
            historyMap = new HashMap<>();
        } else {
           historyMap =  levelHistory.stream()
                    .collect(Collectors.groupingBy(h -> h.getAchievement().getId()));
        }
        
        List<AchievementDTO> result = new ArrayList<>(achievements.size());
        for (int i = 0; i < achievements.size(); i++) {
            Achievement achievement = achievements.get(i);
            AchievementProgress achievementProgress = progress.get(i);
            List<AchievementLevelHistory> history = historyMap.getOrDefault(achievement.getId(), Collections.emptyList());
            AchievementDTO dto = toDto(achievement, achievementProgress, history);
            result.add(dto);
        }

        return result;
    }
}
