package org.borg.backend.story.service;

import jakarta.persistence.EntityNotFoundException;
import org.borg.backend.chapter.mapper.ChapterMapper;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.player.mapper.PlayerProgressMapper;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.story.dto.StoryDTO;
import org.borg.backend.story.dto.StoryOverviewDTO;
import org.borg.backend.story.dto.StoryOverviewRequest;
import org.borg.backend.story.mapper.StoryMapper;
import org.borg.backend.story.model.Story;
import org.borg.backend.story.repository.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryService {
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final PlayerProgressRepository playerProgressRepository;

    public StoryService(StoryRepository storyRepository, ChapterRepository chapterRepository, PlayerProgressRepository playerProgressRepository) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.playerProgressRepository = playerProgressRepository;
    }

    public List<StoryDTO> getAllStories() {
        return StoryMapper.multipleToDTO(storyRepository.findAll());
        
    }

    public StoryOverviewDTO getStoryOverview(StoryOverviewRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new EntityNotFoundException("Story not found"));
        
        List<Chapter> chapters = chapterRepository.findByStoryId(story.getId());

        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), story.getId());
        
        return StoryOverviewDTO.builder()
                .storyId(story.getId())
                .title(story.getTitle())
                .chapters(ChapterMapper.multipleToDTO(chapters))
                .playerProgress(PlayerProgressMapper.toDTO(playerProgress)).
                build();
    }
}
