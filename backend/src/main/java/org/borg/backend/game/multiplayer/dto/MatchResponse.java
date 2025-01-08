package org.borg.backend.game.multiplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchResponse {
    private Long originalSenderId;
    private String playerDisplayName;
    private Long pendingSessionId;
    private Long notificationId;
    private boolean isRematch;
}
