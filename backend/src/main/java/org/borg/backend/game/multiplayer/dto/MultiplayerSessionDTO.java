package org.borg.backend.game.multiplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.game.shared.enums.GameStatus;

@Setter
@Getter
@Builder
public class MultiplayerSessionDTO {
    private Long id;
    private SessionPlayerDTO playerDTO;
    private SessionPlayerDTO opponentDTO;
    private GameStatus status;
    private Long currentPlayerTurn;
}
