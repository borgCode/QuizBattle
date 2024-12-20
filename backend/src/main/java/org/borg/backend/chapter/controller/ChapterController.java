package org.borg.backend.chapter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.dto.InitiateProgressRequest;
import org.borg.backend.chapter.dto.InitiateProgressResponse;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("chapter")
@Tag(name="Chapter")
public class ChapterController {

    private final ChapterService chapterService;
    
    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDTO> getChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok().body(chapterService.getChapter(chapterId));
    }
    
    @PostMapping("/progress/start") 
    public ResponseEntity<InitiateProgressResponse> initiateProgress(@RequestBody InitiateProgressRequest request) {
        return ResponseEntity.ok().body(chapterService.initiateProgress(request));
    }
    
}
