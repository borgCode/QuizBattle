package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.model.ProgressStatus;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class PlayerProgressDTO {
    private Long id;
    private Long currentChapterId;
    private Integer completedChapters;
    private LocalDate startedAt;
    private LocalDate lastPlayed;
    private LocalDate completedAt;
    private ProgressStatus progressStatus;
}
