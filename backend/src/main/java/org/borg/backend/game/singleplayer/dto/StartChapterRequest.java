package org.borg.backend.game.singleplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StartChapterRequest {
    private Long playerId;
    private Long storyId;
    private Long chapterId;
}
