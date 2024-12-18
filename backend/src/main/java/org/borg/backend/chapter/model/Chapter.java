package org.borg.backend.chapter.model;


import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.story.model.Story;

import java.util.HashSet;
import java.util.Set;

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

    @ElementCollection()
    @CollectionTable(name = "chapter_categories", joinColumns = @JoinColumn(name = "chapter_id"))
    @Column(name = "category")
    private Set<String> categories = new HashSet<>();
    
    private String unlockCondition;
    private String rewardText;
    private String imagePath;
    
}
