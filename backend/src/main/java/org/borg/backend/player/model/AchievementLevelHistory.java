package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class AchievementLevelHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn (name = "achievement_id")
    private Achievement achievement;

    @ManyToOne
    @JoinColumn (name = "achievement_level_id")
    private AchievementLevel achievedLevel;
    
    private Instant achievedAt;
}
