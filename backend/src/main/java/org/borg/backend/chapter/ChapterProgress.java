package org.borg.backend.chapter;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.common.enums.ProgressStatus;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChapterProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_progress_id")
    private PlayerProgress playerProgress;

    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    private LocalDate startedAt;
    private LocalDate lastPlayedAt;
    private LocalDate completedAt;

    @Enumerated(EnumType.STRING)
    private ProgressStatus progressStatus;
}
