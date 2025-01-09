package org.borg.backend.game.multiplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MultiplayerQuestionsRequest {
    private String category;
    private Long sessionId;
    private Long playerId;
}
