package org.borg.backend.player.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
