package org.borg.backend.player.dto;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.common.enums.ProgressStatus;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class PlayerProgressDTO {
    private Long currentChapterId;
    private Integer completedChapters;
    private LocalDate startedAt;
    private LocalDate lastPlayed;
    private LocalDate completedAt;
    private ProgressStatus progressStatus;
}
