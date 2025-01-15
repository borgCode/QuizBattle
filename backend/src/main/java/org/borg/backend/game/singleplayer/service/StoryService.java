package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.singleplayer.dto.AllStoriesDTO;
import org.borg.backend.game.singleplayer.dto.StoryDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewRequest;
import org.borg.backend.game.singleplayer.mapper.ChapterMapper;
import org.borg.backend.game.singleplayer.mapper.StoryMapper;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.player.dto.PlayerProgressDTO;
import org.borg.backend.player.mapper.PlayerProgressMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.shared.util.ImageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final PlayerService playerService;
    private final ChapterMapper chapterMapper;
    private final PlayerProgressMapper playerProgressMapper;
    private final StoryMapper storyMapper;

    @Transactional
    public AllStoriesDTO getAllStories(Long playerId) {
        log.debug("Fetching all stories and progress for player {}", playerId);
        
        List<Story> stories = storyRepository.findAll();
        log.debug("Found {} stories in total", stories.size());
                
        List<PlayerProgress> existingProgress = playerProgressRepository.findAllByPlayerId(playerId);
        log.debug("Found {} existing progress entries for player {}", existingProgress.size(), playerId);

        Map<Long, PlayerProgress> progressByStoryId = existingProgress.stream()
                .collect(Collectors.toMap(
                        playerProgress -> playerProgress.getStory().getId(),
                        playerProgress -> playerProgress
                ));

        List<PlayerProgress> newProgress = stories.stream()
                .filter(story -> !progressByStoryId.containsKey(story.getId()))
                .map(story -> createInitialProgress(playerId, story))
                .toList();

        if (!newProgress.isEmpty()) {
            log.info("Creating initial progress for {} new stories for player {}",
                    newProgress.size(), playerId);
            playerProgressRepository.saveAll(newProgress);
            newProgress.forEach(progress -> progressByStoryId.put(progress.getStory().getId(), progress));
        } else {
            log.debug("No new progress entries needed for player {}", playerId);
        }

        List<PlayerProgressDTO> playerProgressDTOS = stories.stream()
                .map(story -> playerProgressMapper.toDTO(progressByStoryId.get(story.getId())))
                .collect(Collectors.toList());

        return new AllStoriesDTO(storyMapper.multipleToDto(stories), playerProgressDTOS);
    }

    private PlayerProgress createInitialProgress(Long playerId, Story story) {
        log.debug("Creating initial progress for story {} and player {}", story.getId(), playerId);
        
        Player player = playerService.getPlayerById(playerId);
        return PlayerProgress.builder()
                .player(player)
                .story(story)
                .completedChapters(0)
                .progressStatus(ProgressStatus.NOT_STARTED)
                .build();
    }

    public StoryOverviewDTO getStoryOverview(StoryOverviewRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Story not found for " + request.getStoryId()));

        List<Chapter> chapters = chapterRepository.findByStoryId(story.getId());

        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), story.getId());

        return StoryOverviewDTO.builder()
                .storyId(story.getId())
                .title(story.getTitle())
                .chapters(chapterMapper.multipleToNoCategoriesDTO(chapters))
                .playerProgress(playerProgressMapper.toDTO(playerProgress)).
                build();
    }
}
