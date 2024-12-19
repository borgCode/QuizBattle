package org.borg.backend.chapter.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
public class ChapterDTO {
    private Long id;
    private Integer chapterNumber;
    private Integer roundWinCondition;
    private String title;
    private String rewardText;
    private String base64Image;
    private Set<String> categories;
}
