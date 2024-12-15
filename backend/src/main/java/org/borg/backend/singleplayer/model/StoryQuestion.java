package org.borg.backend.singleplayer.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class StoryQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String question;
    @ElementCollection
    private List<String> options;
    
    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    private String correctAnswer;
}
