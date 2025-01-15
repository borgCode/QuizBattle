package org.borg.backend.game.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "questions",
        indexes = {
                @Index(name = "IX_category", columnList = "category")
        })
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String question;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> options;
    private String correctAnswer;
}
