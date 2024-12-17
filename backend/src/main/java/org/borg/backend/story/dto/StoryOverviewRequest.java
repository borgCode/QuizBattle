package org.borg.backend.story.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoryOverviewRequest {
    private Long playerId;
    private Long storyId;
}
