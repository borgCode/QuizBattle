package org.borg.backend.chapter.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChapterNoCategoriesDTO {
    private Long id;
    private Integer chapterNumber;
    private String title;
    private String description;
    private String unlockCondition;
    private String base64Image;
}
