package org.borg.backend.multiplayer.model;

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
