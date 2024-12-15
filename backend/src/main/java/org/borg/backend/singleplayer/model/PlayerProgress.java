package org.borg.backend.singleplayer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.player.model.Player;
import org.borg.backend.singleplayer.enums.ProgressStatus;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PlayerProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "story_id")
    private Story story;
    private Long currentChapterId;

    @ElementCollection
    private List<Long> completedChapters;

    private LocalDate startedAt;
    private LocalDate lastPlayed;
    private LocalDate completedAt;

    @Enumerated(EnumType.STRING)
    private ProgressStatus progressStatus;
}
