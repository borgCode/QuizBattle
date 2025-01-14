package org.borg.backend.game.multiplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;

import java.util.Map;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class GameStateResponse {
    private Long playerTurn;
    private List<PlayerDTO> playerDTOS;
    private Integer currentQuestionIndex;
    private Map<Long, Integer> scores;
    private GameStatus status;
    private Set<PlayerQuestionResult> questionResults;
    private List<Long> questionIds;
    private List<String> roundCategories;
    private Map<Long, Boolean> playerAcknowledgment;
    private Long playerWhoGaveUp;
    private Long winnerId;
    private Long loserId;
    private Boolean isTie;
}
