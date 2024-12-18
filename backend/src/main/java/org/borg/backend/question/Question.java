package org.borg.backend.question;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.chapter.model.Chapter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    private String question;
    @ElementCollection
    private List<String> options;
    private String correctAnswer;
}
