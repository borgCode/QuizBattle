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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface AchievementMapper {

    @Mapping(target = "unlockedLevels", expression = "java(mapLevelHistory(levelHistory))")
    @Mapping(target = "totalLevels", expression = "java(achievement.getLevels().size())")
    @Mapping(target = "nextLevel", expression = "java(getNextLevel(achievement, progress))")
    @Mapping(target = "progress", source = "progress", qualifiedByName = "progress")
    @Mapping(target = "name", source = "achievement.name")
    AchievementDTO toDto(
            Achievement achievement,
            AchievementProgress progress,
            List<AchievementLevelHistory> levelHistory
    );
    default List<AchievementDTO> multipleToDto(
            List<Achievement> achievements,
            List<AchievementProgress> progress,
            List<AchievementLevelHistory> levelHistory
    ) {
        if (achievements == null || progress == null || levelHistory == null) {
            return new ArrayList<>();
        }

        return IntStream.range(0, achievements.size())
                .mapToObj(i -> {
                    Achievement currentAchievement = achievements.get(i);
                    List<AchievementLevelHistory> relevantHistory = levelHistory.stream()
                            .filter(history -> {
                                boolean matches = history.getAchievement().getId()
                                        .equals(currentAchievement.getId());
                                System.out.println("Checking achievement " +
                                        currentAchievement.getId() + " against history " +
                                        history.getAchievement().getId() + ": " + matches);
                                return matches;
                            })
                            .collect(Collectors.toList());

                    System.out.println("Achievement " + currentAchievement.getId() +
                            " has " + relevantHistory.size() + " history records");

                    return toDto(
                            currentAchievement,
                            progress.get(i),
                            relevantHistory
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Mapping(target = "name", source = "achievementLevel.name")
    @Mapping(target = "description", source = "achievementLevel.description")
    @Mapping(target = "imageUrl", source = "achievementLevel.imageUrl")
    @Mapping(target = "achievedAt", source = "achievedAt")
    default AchievementLevelDTO toLevelDto(AchievementLevelHistory levelHistory) {
        if (levelHistory == null) return null;
        AchievementLevelDTO dto = AchievementLevelDTO.builder()
                .name(levelHistory.getAchievementLevel().getName())
                .description(levelHistory.getAchievementLevel().getDescription())
                .imageUrl(ImageUtil.encodeAchievementImageToBase64(levelHistory.getAchievementLevel().getImageUrl()))
                .achievedAt(levelHistory.getAchievedAt())
                .build();
        System.out.println("Mapped level history to DTO: " + dto.getName());
        return dto;
    }
    
    default AchievementLevelDTO toLevelDto(AchievementLevel level) {
        if (level == null) return null;
        return AchievementLevelDTO.builder()
                .name(level.getName())
                .description(level.getDescription())
                .imageUrl(ImageUtil.encodeAchievementImageToBase64(level.getImageUrl()))
                .build();
    }

    default List<AchievementLevelDTO> mapLevelHistory(List<AchievementLevelHistory> levelHistory) {
        if (levelHistory == null) {
            System.out.println("Level history is null");
            return new ArrayList<>();
        }
        List<AchievementLevelDTO> mappedLevels = levelHistory.stream()
                .map(history -> {
                    AchievementLevelDTO dto = toLevelDto(history);
                    System.out.println("Mapped level: " + dto.getName() + " for achievement");
                    return dto;
                })
                .collect(Collectors.toList());
        System.out.println("Mapped " + mappedLevels.size() + " levels");
        return mappedLevels;
    }

    @Named("nextLevel")
    default AchievementLevelDTO getNextLevel(Achievement achievement, AchievementProgress progress) {
        if (progress == null || progress.getCurrentLevel() == null) {
            return achievement.getLevels().isEmpty() ? null : toLevelDto(achievement.getLevels().get(0));
        }
        return toLevelDto(achievement.getNextLevel(progress.getCurrentLevel()));
    }

    @Named("progress")
    default AchievementProgressDTO toProgressDto(AchievementProgress progress) {
        return AchievementProgressDTO.builder()
                .currentProgress(progress != null ? progress.getCurrentProgress() : 0)
                .requirementForNext(progress != null ? progress.getNextLevelRequirement() : 0)
                .progressPercentage(progress != null && progress.getNextLevelRequirement() > 0 ?
                        (progress.getCurrentProgress() * 100.0) / progress.getNextLevelRequirement() : 0.0)
                .build();
    }
    
}
