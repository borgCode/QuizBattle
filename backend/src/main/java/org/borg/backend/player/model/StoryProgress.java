package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.game.singleplayer.model.Story;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class StoryProgress {
    
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
    
    private Integer completedChapters;

    private LocalDate startedAt;
    private LocalDate lastPlayed;
    private LocalDate completedAt;

    @Enumerated(EnumType.STRING)
    private ProgressStatus progressStatus;
}
