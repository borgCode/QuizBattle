package org.borg.backend.question.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MultiplayerAnswerValidationRequest {
    private Long questionId;
    private Long sessionId;
    private String answer;
    private Long playerId;
}
