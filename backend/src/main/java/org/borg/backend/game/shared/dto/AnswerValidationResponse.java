package org.borg.backend.game.shared.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnswerValidationResponse {
    private boolean isCorrect;
    private int correctAnswerIndex;
}
