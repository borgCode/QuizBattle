package org.borg.backend.game.shared.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PlayerQuestionResult {
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "question_index")
    private Integer questionIndex;
    @Column(name = "correct")
    private boolean correct;
    
}
