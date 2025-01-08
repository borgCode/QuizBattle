package org.borg.backend.game.multiplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchDecision {
    private Long matchmakingSessionId;
    private Long playerId;
}
