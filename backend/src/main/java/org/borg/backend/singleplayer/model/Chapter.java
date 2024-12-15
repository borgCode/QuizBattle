package org.borg.backend.singleplayer.model;


import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.question.Question;
import org.borg.backend.singleplayer.enums.ProgressStatus;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "chapters")
public class Chapter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "story_id")
    private Story story;
    
    private Integer chapterNumber;
    private String title;
    private String description;
    
    @OneToMany(mappedBy = "chapter")
    private List<StoryQuestion> questions;
    
    private String unlockCondition;
    private String rewardText;
    private String imagePath;
    
}
