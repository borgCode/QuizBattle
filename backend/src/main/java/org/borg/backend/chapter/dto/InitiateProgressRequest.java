package org.borg.backend.chapter.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InitiateProgressRequest {
    private Long playerId;
    private Long playerProgressId;
    private Long storyId;
    private Long chapterId;
}
