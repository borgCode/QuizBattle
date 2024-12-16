package org.borg.backend.story.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.story.dto.StoryDTO;
import org.borg.backend.story.service.StoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("story")
@Tag(name = "Story")
public class StoryController {

    private final StoryService storyService;

    @GetMapping("/all")
    public ResponseEntity<List<StoryDTO>> getAllStories() {
        return ResponseEntity.ok().body(storyService.getAllStories());
    }
}
