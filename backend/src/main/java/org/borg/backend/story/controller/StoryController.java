package org.borg.backend.story.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.story.dto.AllStoriesDTO;
import org.borg.backend.story.dto.StoryOverviewDTO;
import org.borg.backend.story.dto.StoryOverviewRequest;
import org.borg.backend.story.service.StoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("story")
@Tag(name = "Story")
public class StoryController {

    private final StoryService storyService;

    @PostMapping("/all/{playerId}")
    public ResponseEntity<AllStoriesDTO> getAllStories(@PathVariable Long playerId) {
        return ResponseEntity.ok().body(storyService.getAllStories(playerId));
    }
    
    @PostMapping("/story-overview")
    public ResponseEntity<StoryOverviewDTO> getStoryOverview(@RequestBody StoryOverviewRequest request) {
        return ResponseEntity.ok().body(storyService.getStoryOverview(request));
    }
}
