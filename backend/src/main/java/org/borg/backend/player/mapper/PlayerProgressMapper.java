package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.PlayerProgressDTO;
import org.borg.backend.player.model.PlayerProgress;

public class PlayerProgressMapper {

    public static PlayerProgressDTO toDTO(PlayerProgress playerProgress) {
        if (playerProgress == null) {
            return null;
        }

        return PlayerProgressDTO.builder()
                .id(playerProgress.getId())
                .currentChapterId(playerProgress.getCurrentChapterId())
                .completedChapters(playerProgress.getCompletedChapters())
                .startedAt(playerProgress.getStartedAt())
                .lastPlayed(playerProgress.getLastPlayed())
                .completedAt(playerProgress.getCompletedAt())
                .progressStatus(playerProgress.getProgressStatus())
                .build();
        
    }
}
