package org.borg.backend.multiplayer.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RematchResponse {
    private Long playerId;
    private String playerDisplayName;
    private Long pendingSessionId;
    private Long notificationId;
    private boolean hasAccepted;
}
