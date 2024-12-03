package org.borg.backend.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.multiplayer.GameStateResponse;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GameQuestionsResponse {
    private List<QuestionDTO> questions;
    private GameStateResponse gameStateResponse;
}
