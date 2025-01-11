package org.borg.backend.chapter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.dto.InitiateProgressRequest;
import org.borg.backend.chapter.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("chapter")
@Tag(name="Chapter")
public class ChapterController {

    private final ChapterService chapterService;
    
    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDTO> getChapter(@PathVariable long chapterId) {
        return ResponseEntity.ok().body(chapterService.getChapter(chapterId));
    }


    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PostMapping("/progress/start") 
    public ResponseEntity<Void> startChapter(@RequestBody InitiateProgressRequest request) {
        chapterService.startChapter(request);
        return ResponseEntity.ok().build();
    }
    
    //TODO should not be an endpoint
    
    @PostMapping("/progress/update/{chapterProgressId}")
    public ResponseEntity<Void> updateChapterProgress(@PathVariable long chapterProgressId) {
        chapterService.updateChapterProgress(chapterProgressId);
        return ResponseEntity.ok().build();
    }
    
}
