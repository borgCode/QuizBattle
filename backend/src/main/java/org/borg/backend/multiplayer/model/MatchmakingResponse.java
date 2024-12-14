package org.borg.backend.multiplayer.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.borg.backend.multiplayer.enums.MatchStatus;

/**
 * Response object for matchmaking operations.
 * Field usage by status:
 * - WAITING: No additional fields
 * - MATCHED: pendingSessionId, opponentDisplayName
 * - ACCEPTED: sessionId, opponentDisplayName
 * - DECLINED: No additional fields
 */

@Getter
@AllArgsConstructor
public class MatchmakingResponse {
    private MatchStatus matchStatus;
    private Long pendingSessionId;
    private Long sessionId;
    private String opponentDisplayName;

    public static MatchmakingResponse waiting() {
        return new MatchmakingResponse(MatchStatus.WAITING, null, null, null);
    }

    public static MatchmakingResponse matched(Long pendingSessionId, String opponentDisplayName) {
        return new MatchmakingResponse(MatchStatus.MATCHED, pendingSessionId, null, opponentDisplayName);
    }

    public static MatchmakingResponse accepted(Long sessionId, String opponentDisplayName) {
        return new MatchmakingResponse(MatchStatus.ACCEPTED, null, sessionId, opponentDisplayName);
    }

    public static MatchmakingResponse declined() {
        return new MatchmakingResponse(MatchStatus.DECLINED, null, null, null);
    }
}
