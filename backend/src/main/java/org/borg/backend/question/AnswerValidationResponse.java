package org.borg.backend.question;


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
