package org.borg.backend.multiplayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class GameStateUpdate {
    private Integer currentQuestionIndex;
    private Map<Long, Integer> scores;
    private GameStatus status;
    private Boolean isOpponentTurn;
}
