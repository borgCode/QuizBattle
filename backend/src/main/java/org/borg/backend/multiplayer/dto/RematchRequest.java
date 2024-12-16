package org.borg.backend.multiplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RematchRequest {
    
    private Long sessionId;
    private Long playerId;
    private Long notificationId;
}
