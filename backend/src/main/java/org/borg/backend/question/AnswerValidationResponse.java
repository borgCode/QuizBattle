package org.borg.backend.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.multiplayer.GameStateUpdate;

@Getter
@Setter
@AllArgsConstructor
public class AnswerValidationResponse {
    private boolean correct;
    private GameStateUpdate gameStateUpdate;
}
