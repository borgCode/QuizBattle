package org.borg.backend.story.service;

import org.borg.backend.story.dto.StoryDTO;
import org.borg.backend.story.mapper.StoryMapper;
import org.borg.backend.story.repository.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryService {
    private final StoryRepository storyRepository;

    public StoryService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    public List<StoryDTO> getAllStories() {
        return StoryMapper.multipleToDTO(storyRepository.findAll());
        
    }
}
