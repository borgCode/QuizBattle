package org.borg.backend.question.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SinglePlayerAnswerValidationRequest {
    private Long questionId;
    private Integer index;
    private String answer;
    private Long playerId;
}
