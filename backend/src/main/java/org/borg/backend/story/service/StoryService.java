package org.borg.backend.story.service;

import jakarta.persistence.EntityNotFoundException;
import org.borg.backend.chapter.mapper.ChapterMapper;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.common.enums.ProgressStatus;
import org.borg.backend.common.util.ImageUtil;
import org.borg.backend.player.dto.PlayerProgressDTO;
import org.borg.backend.player.mapper.PlayerProgressMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.story.dto.AllStoriesDTO;
import org.borg.backend.story.dto.StoryDTO;
import org.borg.backend.story.dto.StoryOverviewDTO;
import org.borg.backend.story.dto.StoryOverviewRequest;
import org.borg.backend.story.model.Story;
import org.borg.backend.story.repository.StoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoryService {
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final PlayerRepository playerRepository;

    public StoryService(StoryRepository storyRepository, ChapterRepository chapterRepository, PlayerProgressRepository playerProgressRepository, PlayerRepository playerRepository) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.playerProgressRepository = playerProgressRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public AllStoriesDTO getAllStories(Long playerId) {

        List<Story> stories = storyRepository.findAll();


        List<StoryDTO> storyDTOS = new ArrayList<>();
        List<PlayerProgressDTO> playerProgressDTOS = new ArrayList<>();

        for (Story story : stories) {
            PlayerProgressDTO playerProgressDTO = getOrCreatePlayerProgress(playerId, story);
            playerProgressDTOS.add(playerProgressDTO);
            
            StoryDTO storyDTO = StoryDTO.builder()
                    .id(story.getId())
                    .title(story.getTitle())
                    .description(story.getDescription())
                    .introText(story.getIntroText())
                    .numOfChapters(story.getNumOfChapters())
                    .base64Image(ImageUtil.encodeStoryImageToBase64(story.getImagePath()))
                    .build();
            storyDTOS.add(storyDTO);
        }


        return new AllStoriesDTO(storyDTOS, playerProgressDTOS);

    }

    private PlayerProgressDTO getOrCreatePlayerProgress(Long playerId, Story story) {
        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(playerId, story.getId());
        
        if (playerProgress == null) {
            Player player = playerRepository.findById(playerId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
            playerProgress = playerProgressRepository.save(PlayerProgress.builder()
                    .player(player)
                    .story(story)
                    .completedChapters(0)
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build());
        }
        return PlayerProgressMapper.toDTO(playerProgress);
    }

    public StoryOverviewDTO getStoryOverview(StoryOverviewRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new EntityNotFoundException("Story not found"));

        List<Chapter> chapters = chapterRepository.findByStoryId(story.getId());

        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), story.getId());

        return StoryOverviewDTO.builder()
                .storyId(story.getId())
                .title(story.getTitle())
                .chapters(ChapterMapper.multipleToNoCategoriesDTO(chapters))
                .playerProgress(PlayerProgressMapper.toDTO(playerProgress)).
                build();
    }
}
