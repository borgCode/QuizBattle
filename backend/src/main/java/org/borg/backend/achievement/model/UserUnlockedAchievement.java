package org.borg.backend.achievement.model;


import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.player.model.Player;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class UserUnlockedAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn (name = "player_id")
    private Player player;
    
    @ManyToOne
    @JoinColumn (name = "achievement_id")
    private Achievement achievement;
    
    @ManyToOne
    @JoinColumn (name = "achievement_level_id")
    private AchievementLevel currentLevel;
    
    
    private LocalDateTime achievedAt;
}
