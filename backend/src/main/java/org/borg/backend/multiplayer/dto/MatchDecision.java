package org.borg.backend.multiplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchDecision {
    private Long pendingSessionId;
    private Long playerId;
}
