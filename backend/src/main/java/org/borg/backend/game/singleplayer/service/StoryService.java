package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.singleplayer.dto.AllStoriesDTO;
import org.borg.backend.game.singleplayer.dto.StoryDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewDTO;
import org.borg.backend.game.singleplayer.dto.StoryOverviewRequest;
import org.borg.backend.game.singleplayer.mapper.ChapterMapper;
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

    @Transactional
    public AllStoriesDTO getAllStories(Long playerId) {

        List<Story> stories = storyRepository.findAll();

        List<StoryDTO> storyDTOS = new ArrayList<>();
        List<PlayerProgressDTO> playerProgressDTOS = new ArrayList<>();

        for (Story story : stories) {
            PlayerProgressDTO playerProgressDTO = playerProgressMapper.toDTO(getOrCreatePlayerProgress(playerId, story));
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

    public PlayerProgress getOrCreatePlayerProgress(Long playerId, Story story) {
        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(playerId, story.getId());

        if (playerProgress == null) {
            Player player = playerService.getPlayerById(playerId);;

            playerProgress = playerProgressRepository.save(PlayerProgress.builder()
                    .player(player)
                    .story(story)
                    .completedChapters(0)
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build());
        }
        return playerProgress;
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
