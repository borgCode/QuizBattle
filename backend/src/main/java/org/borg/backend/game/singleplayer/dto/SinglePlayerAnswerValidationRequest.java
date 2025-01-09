package org.borg.backend.game.singleplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SinglePlayerAnswerValidationRequest {
    private Long questionId;
    private String answer;
    private Long playerId;
}
