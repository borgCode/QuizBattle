package org.borg.backend.multiplayer;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchmakingResponse {
    private MatchStatus matchStatus;
    private Long sessionId;
    private String opponentDisplayName;
    
}
