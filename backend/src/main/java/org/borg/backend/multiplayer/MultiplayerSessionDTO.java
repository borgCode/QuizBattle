package org.borg.backend.multiplayer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.PlayerDTO;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
public class MultiplayerSessionDTO {
    private List<PlayerDTO> playerDTOList;
    private Map<Long, Integer> score;
    private GameStatus status;
    private PlayerDTO currentPlayerTurn;
}
