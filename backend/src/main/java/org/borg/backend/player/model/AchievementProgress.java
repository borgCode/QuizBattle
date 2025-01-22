package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class AchievementProgress {

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
    private AchievementLevel currentLevel;
    
    private int currentProgress;
    private int nextLevelRequirement;
}
