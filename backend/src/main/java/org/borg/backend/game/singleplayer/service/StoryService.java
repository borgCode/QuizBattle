package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.singleplayer.dto.AllStoriesDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewRequest;
import org.borg.backend.game.singleplayer.mapper.ChapterMapper;
import org.borg.backend.game.singleplayer.mapper.StoryMapper;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.player.dto.StoryProgressDTO;
import org.borg.backend.player.mapper.StoryProgressMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.StoryProgress;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.repository.StoryProgressRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryProgressRepository storyProgressRepository;
    private final PlayerService playerService;
    private final ChapterMapper chapterMapper;
    private final StoryProgressMapper storyProgressMapper;
    private final StoryMapper storyMapper;

    @Transactional
    public AllStoriesDTO getAllStories(Long playerId) {
        log.debug("Fetching all stories and progress for player {}", playerId);
        
        List<Story> stories = storyRepository.findAll();
        log.debug("Found {} stories in total", stories.size());
                
        List<StoryProgress> existingProgress = storyProgressRepository.findAllByPlayerId(playerId);
        log.debug("Found {} existing progress entries for player {}", existingProgress.size(), playerId);

        Map<Long, StoryProgress> progressByStoryId = existingProgress.stream()
                .collect(Collectors.toMap(
                        storyProgress -> storyProgress.getStory().getId(),
                        storyProgress -> storyProgress
                ));

        List<StoryProgress> newProgress = stories.stream()
                .filter(story -> !progressByStoryId.containsKey(story.getId()))
                .map(story -> createInitialProgress(playerId, story))
                .toList();

        if (!newProgress.isEmpty()) {
            log.info("Creating initial progress for {} new stories for player {}",
                    newProgress.size(), playerId);
            storyProgressRepository.saveAll(newProgress);
            newProgress.forEach(progress -> progressByStoryId.put(progress.getStory().getId(), progress));
        } else {
            log.debug("No new progress entries needed for player {}", playerId);
        }

        List<StoryProgressDTO> storyProgressDTOS = stories.stream()
                .map(story -> storyProgressMapper.toDTO(progressByStoryId.get(story.getId())))
                .collect(Collectors.toList());

        return new AllStoriesDTO(storyMapper.multipleToDto(stories), storyProgressDTOS);
    }

    private StoryProgress createInitialProgress(Long playerId, Story story) {
        log.debug("Creating initial progress for story {} and player {}", story.getId(), playerId);
        
        Player player = playerService.getPlayerById(playerId);
        return StoryProgress.builder()
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

        StoryProgress storyProgress = storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), story.getId());

        return StoryOverviewDTO.builder()
                .storyId(story.getId())
                .title(story.getTitle())
                .chapters(chapterMapper.multipleToChapterOverviewDTO(chapters))
                .storyProgressDTO(storyProgressMapper.toDTO(storyProgress)).
                build();
    }
}
