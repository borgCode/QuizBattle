package org.borg.backend.game.singleplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.singleplayer.dto.PlayChapterDTO;
import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.mapper.ChapterMapper;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.model.StoryProgress;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.repository.StoryProgressRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final StoryProgressRepository storyProgressRepository;
    private final ChapterProgressRepository chapterProgressRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChapterSessionService chapterSessionService;
    private final ChapterMapper chapterMapper;

    public PlayChapterDTO getChapter(Long chapterId) {
        log.debug("Fetching chapter with ID: {}", chapterId);

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> {
                    log.error("Chapter not found with ID: {}", chapterId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Chapter not found for: " + chapterId);
                });

        log.debug("Successfully retrieved chapter: {}", chapter.getId());
        return chapterMapper.toPlayChapterDTO(chapter);
    }

    @Transactional
    public void startChapter(StartChapterRequest request) {
        log.info("Starting chapter for player: {}, story: {}, chapter: {}",
                request.getPlayerId(), request.getStoryId(), request.getChapterId());

        StoryProgress storyProgress = storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId());
        log.debug("Found story progress for player: {}, current status: {}",
                request.getPlayerId(), storyProgress.getProgressStatus());

        storyProgress.setStartedAt(LocalDate.now());
        storyProgress.setProgressStatus(ProgressStatus.IN_PROGRESS);
        storyProgress.setCurrentChapterId(request.getChapterId());

        storyProgress = storyProgressRepository.save(storyProgress);
        log.debug("Updated story progress status to IN_PROGRESS");

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> {
                    log.error("Chapter not found with ID: {}", request.getChapterId());
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Chapter not found for: " + request.getChapterId());
                });

        log.debug("Initializing chapter session for player: {}", request.getPlayerId());
        chapterSessionService.initializeChapterSession(request.getPlayerId(), chapter);

        ChapterProgress chapterProgress = chapterProgressRepository.findByStoryProgressIdAndChapterId(storyProgress.getId(), chapter.getId());

        if (chapterProgress == null) {
            log.debug("Creating new chapter progress for player: {} and chapter: {}",
                    request.getPlayerId(), chapter.getId());

            chapterProgress = ChapterProgress.builder()
                    .storyProgress(storyProgress)
                    .chapter(chapter)
                    .startedAt(LocalDate.now())
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .build();

            chapterProgressRepository.save(chapterProgress);
            log.info("Successfully initiated chapter progress for player: {}, chapter: {}",
                    request.getPlayerId(), chapter.getId());
        } else {
            log.debug("Chapter progress already exists for player: {} and chapter: {}",
                    request.getPlayerId(), chapter.getId());
        }
    }

    @Transactional
    public void updateChapterProgress(Long chapterProgressId) {
        log.info("Updating chapter progress with ID: {}", chapterProgressId);

        ChapterProgress chapterProgress = chapterProgressRepository.findById(chapterProgressId)
                .orElseThrow(() -> {
                    log.error("Chapter progress not found with ID: {}", chapterProgressId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Chapter progress not found for " + chapterProgressId);
                });

        if (chapterProgress.getProgressStatus().equals(ProgressStatus.COMPLETED)) {
            log.debug("Chapter progress {} is already completed, skipping update", chapterProgressId);
            return;
        }

        chapterProgress.setCompletedAt(LocalDate.now());
        chapterProgress.setProgressStatus(ProgressStatus.COMPLETED);

        Long storyProgressId = chapterProgress.getStoryProgress().getId();
        log.debug("Retrieving story progress with ID: {}", storyProgressId);

        StoryProgress storyProgress = storyProgressRepository.findById(storyProgressId)
                .orElseThrow(() -> {
                    log.error("Story progress not found with ID: {}", storyProgressId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            "Story progress not found for " + storyProgressId);
                });

        storyProgress.setCompletedChapters(storyProgress.getCompletedChapters() + 1);
        log.debug("Updated completed chapters count to: {}", storyProgress.getCompletedChapters());

        if (storyProgress.getCompletedChapters() >= storyProgress.getStory().getNumOfChapters()) {
            log.info("Player {} has completed all chapters in story: {}", storyProgress.getPlayer().getId(), storyProgress.getStory().getTitle());

            storyProgress.setCompletedAt(LocalDate.now());
            storyProgress.setProgressStatus(ProgressStatus.COMPLETED);

            applicationEventPublisher.publishEvent(new AchievementEvents.StoryCompletedEvent(
                    storyProgress.getPlayer().getId(),
                    storyProgress.getStory().getTitle()
            ));
            log.debug("Published story completion event for player: {}",
                    storyProgress.getPlayer().getId());
        }

        chapterProgressRepository.save(chapterProgress);
        storyProgressRepository.save(storyProgress);
        log.info("Successfully updated chapter and story progress");
    }
}
