package org.borg.backend.player.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class CategoryStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String category;
    private int correct;
    private int questionsAnswered;

    public CategoryStats(String category) {
        this.category = category;
    }

    public void incrementQuestionsAnswered() {
        this.questionsAnswered++;
    }

    public void incrementCorrectAnswers() {
        this.correct++;
    }
}
