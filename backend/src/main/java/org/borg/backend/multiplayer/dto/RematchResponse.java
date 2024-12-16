package org.borg.backend.multiplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RematchResponse {
    private Long originalSenderId;
    private String playerDisplayName;
    private Long pendingSessionId;
    private Long notificationId;
}
