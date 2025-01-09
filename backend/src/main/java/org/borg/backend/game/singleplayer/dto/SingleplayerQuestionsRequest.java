package org.borg.backend.game.singleplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SingleplayerQuestionsRequest {
    private String category;
    private Long playerId;
}
