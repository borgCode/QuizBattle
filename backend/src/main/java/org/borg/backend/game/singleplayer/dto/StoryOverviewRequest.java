package org.borg.backend.game.singleplayer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoryOverviewRequest {
    private Long playerId;
    private Long storyId;
}
