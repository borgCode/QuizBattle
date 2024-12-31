package org.borg.backend.achievement.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class AchievementLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Achievement achievement;
    
    private String name;
    private int level;
    private int requirementValue;
    private String description;
    private String imageUrl;
    
}
