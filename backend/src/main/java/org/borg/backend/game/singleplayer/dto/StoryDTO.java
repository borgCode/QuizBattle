package org.borg.backend.game.singleplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class StoryDTO {

    private Long id;
    private String title;
    private String description;
    private String introText;
    private Integer numOfChapters;
    private String base64Image;
}
