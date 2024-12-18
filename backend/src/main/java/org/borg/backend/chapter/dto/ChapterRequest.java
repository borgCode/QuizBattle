package org.borg.backend.chapter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChapterRequest {
    private Long storyId;
    private Long chapterId;
}
