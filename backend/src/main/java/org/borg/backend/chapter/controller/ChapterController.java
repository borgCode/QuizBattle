package org.borg.backend.chapter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.dto.InitiateProgressRequest;
import org.borg.backend.chapter.dto.InitiateProgressResponse;
import org.borg.backend.chapter.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("chapter")
@Tag(name="Chapter")
public class ChapterController {

    private final ChapterService chapterService;
    
    @GetMapping("/{chapterId}") /**TODO Försök använda long allmänt om det går istället för Long.
    TODO Nu borde jag tekniskt sett kunna skicka null som pathVariable och det går igenom. Använd bara det om du faktiskt behöver att den ska kunna vara null.
     **/
    public ResponseEntity<ChapterDTO> getChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok().body(chapterService.getChapter(chapterId));
    }
    
    @PostMapping("/progress/start") 
    public ResponseEntity<InitiateProgressResponse> initiateProgress(@RequestBody InitiateProgressRequest request) {
        return ResponseEntity.ok().body(chapterService.initiateProgress(request));
    }
    
    @PostMapping("/progress/update/{chapterProgressId}") //TODO Försök använda long istället för Long, nu borde jag tekniskt sett kunna skicka null som pathVariable och det går igenom.
    public ResponseEntity<Void> updateChapterProgress(@PathVariable Long chapterProgressId) {
        chapterService.updateChapterProgress(chapterProgressId);
        return ResponseEntity.ok().build();
    }
    
}
