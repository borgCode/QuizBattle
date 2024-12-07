package org.borg.backend.multiplayer.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchmakingResponse {
    private MatchStatus matchStatus;
    private Long pendingSessionId;
    private String opponentDisplayName;
    
}
