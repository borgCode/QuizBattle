package org.borg.backend.multiplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchRequest {
    private Long senderId;
    private Long receiverId;
}
