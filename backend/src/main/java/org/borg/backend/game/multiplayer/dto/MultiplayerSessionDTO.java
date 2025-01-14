package org.borg.backend.game.multiplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.player.dto.PlayerDTO;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
public class MultiplayerSessionDTO {
    private Long id;
    private List<PlayerDTO> playerDTOS;
    private Map<Long, Integer> scores;
    private GameStatus status;
    private PlayerDTO currentPlayerTurn;
}
