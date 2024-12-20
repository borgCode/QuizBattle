package org.borg.backend.chapter.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InitiateProgressResponse {
    private Long playerProgressId;
    private Long chapterProgressId;
}
