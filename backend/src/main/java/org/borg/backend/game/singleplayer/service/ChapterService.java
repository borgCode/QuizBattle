package org.borg.backend.game.singleplayer.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.game.singleplayer.dto.ChapterDTO;
import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.mapper.ChapterMapper;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.model.ProgressStatus;
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
    private final PlayerProgressRepository playerProgressRepository;
    private final ChapterProgressRepository chapterProgressRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChapterSessionService chapterSessionService;

    public ChapterDTO getChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Chapter not found for: " + chapterId));
        return ChapterMapper.toDTO(chapter);
    }

    @Transactional
    public void startChapter(StartChapterRequest request) {
        PlayerProgress playerProgress = playerProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId());

        playerProgress.setStartedAt(LocalDate.now());
        playerProgress.setProgressStatus(ProgressStatus.IN_PROGRESS);
        playerProgress.setCurrentChapterId(request.getChapterId());

        playerProgress = playerProgressRepository.save(playerProgress);

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Chapter not found for: " + request.getChapterId()));

        chapterSessionService.initializeChapterSession(request.getPlayerId(), chapter);

        ChapterProgress chapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(playerProgress.getId(), chapter.getId());

        if (chapterProgress == null) {
            chapterProgress = ChapterProgress.builder()
                    .playerProgress(playerProgress)
                    .chapter(chapter)
                    .startedAt(LocalDate.now())
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .build();

            chapterProgressRepository.save(chapterProgress);
        }
    }

    @Transactional
    public void updateChapterProgress(Long chapterProgressId) {
        ChapterProgress chapterProgress = chapterProgressRepository.findById(chapterProgressId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Chapter progress not found for " + chapterProgressId));

        if (chapterProgress.getProgressStatus().equals(ProgressStatus.COMPLETED)) {
            return;
        }

        chapterProgress.setCompletedAt(LocalDate.now());
        chapterProgress.setProgressStatus(ProgressStatus.COMPLETED);
        
        Long playerProgressId = chapterProgress.getPlayerProgress().getId();

        PlayerProgress playerProgress = playerProgressRepository.findById(playerProgressId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player progress not found for " + playerProgressId));
        
        playerProgress.setCompletedChapters(playerProgress.getCompletedChapters() + 1);

        if (playerProgress.getCompletedChapters() >= playerProgress.getStory().getNumOfChapters()) {
            playerProgress.setCompletedAt(LocalDate.now());
            playerProgress.setProgressStatus(ProgressStatus.COMPLETED);

            applicationEventPublisher.publishEvent(new AchievementEvents.StoryCompletedEvent(
                    playerProgress.getPlayer().getId(),
                    playerProgress.getStory().getTitle()
            ));
        }
        chapterProgressRepository.save(chapterProgress);
        playerProgressRepository.save(playerProgress);
    }
}
