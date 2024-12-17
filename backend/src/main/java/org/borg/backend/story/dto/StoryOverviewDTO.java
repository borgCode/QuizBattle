package org.borg.backend.story.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.player.dto.PlayerProgressDTO;

import java.util.List;

@Getter
@Setter
@Builder
public class StoryOverviewDTO {
    private Long storyId;
    private String title;
    private List<ChapterDTO> chapters;
    private PlayerProgressDTO playerProgress;
    
}
