package org.borg.backend.game.singleplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChapterOverviewDTO {
    private Long id;
    private Integer chapterNumber;
    private String title;
    private String description;
    private String unlockCondition;
    private String base64Image;
}
