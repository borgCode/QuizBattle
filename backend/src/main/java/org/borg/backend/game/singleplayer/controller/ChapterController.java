package org.borg.backend.game.singleplayer.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.game.singleplayer.dto.PlayChapterDTO;
import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.service.ChapterService;
import org.borg.backend.game.singleplayer.service.SinglePlayerQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("chapter")
@Tag(name="Chapter")
public class ChapterController {

    private final ChapterService chapterService;
    private final SinglePlayerQuestionService singlePlayerQuestionService;

    @GetMapping("/{chapterId}")
    public ResponseEntity<PlayChapterDTO> getChapter(@PathVariable long chapterId) {
        return ResponseEntity.ok().body(chapterService.getChapter(chapterId));
    }


    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PostMapping("/start") 
    public ResponseEntity<Void> startChapter(@RequestBody StartChapterRequest request) {
        chapterService.startChapter(request);
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/clear/{playerId}")
    public ResponseEntity<Void> clearChapter(@PathVariable long playerId) {
        singlePlayerQuestionService.clearSession(playerId);
        return ResponseEntity.ok().build();
    }
    
}
