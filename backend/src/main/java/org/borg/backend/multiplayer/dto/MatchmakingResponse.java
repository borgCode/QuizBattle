package org.borg.backend.multiplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.borg.backend.common.enums.MatchStatus;


@Data
@AllArgsConstructor
public class MatchmakingResponse {
    private MatchStatus matchStatus;
    private Long matchmakingSessionId;
    private Long sessionId;
    private String opponentDisplayName;

    public static MatchmakingResponse waiting() {
        return new MatchmakingResponse(MatchStatus.WAITING, null, null, null);
    }
    public static MatchmakingResponse waitingForOtherPlayer() {
        return new MatchmakingResponse(MatchStatus.WAITING_FOR_OTHER_PLAYER, null, null, null);
    }

    public static MatchmakingResponse matched(Long matchMakingSessionId, String opponentDisplayName) {
        return new MatchmakingResponse(MatchStatus.MATCHED, matchMakingSessionId, null, opponentDisplayName);
    }

    public static MatchmakingResponse accepted(Long sessionId, String opponentDisplayName) {
        return new MatchmakingResponse(MatchStatus.ACCEPTED, null, sessionId, opponentDisplayName);
    }

    public static MatchmakingResponse declined() {
        return new MatchmakingResponse(MatchStatus.DECLINED, null, null, null);
    }
}
