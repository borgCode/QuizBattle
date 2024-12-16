package org.borg.backend.story.service;

import org.borg.backend.story.model.Story;
import org.borg.backend.story.repository.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryService {
    private final StoryRepository storyRepository;

    public StoryService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    public List<Story> getAllStories() {
        return storyRepository.findAll();
    }
}
