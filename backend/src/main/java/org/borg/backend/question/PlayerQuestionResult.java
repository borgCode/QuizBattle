package org.borg.backend.question;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class PlayerQuestionResult {
    @Column(name = "player_id")
    private Long playerId;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "question_index")
    private Integer questionIndex;
    @Column(name = "correct")
    private boolean correct;
}
