package org.borg.backend.chapter.model;


import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.question.Question;
import org.borg.backend.story.model.Story;

import java.util.ArrayList;
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
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY)
    private List<Question> questions = new ArrayList<>();
    
    private String unlockCondition;
    private String rewardText;
    private String imagePath;
    
}
