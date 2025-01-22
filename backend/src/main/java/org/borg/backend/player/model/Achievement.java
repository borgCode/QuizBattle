package org.borg.backend.player.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(indexes = {
        @Index(name = "findByName", columnList = "name")
})
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    
    @OneToMany(mappedBy = "achievement", cascade = CascadeType.ALL)
    private List<AchievementLevel> levels = new ArrayList<>();

    public AchievementLevel getLastLevel() {
        return getLevels().get(getLevels().size() - 1);
    }

    public AchievementLevel getNextLevel(AchievementLevel currentLevel) {
        List<AchievementLevel> levels = getLevels();
        int currentIndex = levels.indexOf(currentLevel);
        return currentIndex < levels.size() - 1 ? levels.get(currentIndex + 1) : null;
    }
}
