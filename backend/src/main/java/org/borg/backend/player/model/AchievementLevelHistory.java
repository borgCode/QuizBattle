package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Player player;

    @ManyToOne
    @JoinColumn (name = "achievement_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Achievement achievement;

    @ManyToOne
    @JoinColumn (name = "achievement_level_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AchievementLevel achievementLevel;
    
    private Instant achievedAt;
}
