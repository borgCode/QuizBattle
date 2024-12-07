package org.borg.backend.multiplayer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.PlayerDTO;
import org.borg.backend.question.PlayerQuestionResult;

import java.util.Map;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class GameStateResponse {
    private Long playerTurn;
    private List<PlayerDTO> playerDTOS;
    private Integer currentQuestionIndex;
    private Map<Long, Integer> scores;
    private GameStatus status;
    private Set<PlayerQuestionResult> results;
    private List<Long> questionIds;
}
