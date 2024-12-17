package org.borg.backend.multiplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.player.dto.PlayerDTO;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
public class MultiplayerSessionDTO {
    private Long id;
    private List<PlayerDTO> playerDTOList;
    private Map<Long, Integer> score;
    private GameStatus status;
    private PlayerDTO currentPlayerTurn;
}
