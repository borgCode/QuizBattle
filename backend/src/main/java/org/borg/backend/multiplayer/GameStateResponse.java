package org.borg.backend.multiplayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.PlayerDTO;

import java.util.Map;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GameStateResponse {
    private List<PlayerDTO> playerDTOS;
    private Integer currentQuestionIndex;
    private Map<Long, Integer> scores;
    private GameStatus status;
}
