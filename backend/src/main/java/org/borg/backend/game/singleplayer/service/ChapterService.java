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
import org.borg.backend.shared.enums.ProgressStatus;
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
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
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
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));

        chapterSessionService.initializeChapterSession(request.getPlayerId(), chapter);

        ChapterProgress chapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(playerProgress.getId(), chapter.getId());

        if (chapterProgress == null) {
            log.warn("Creating new chapter progress");
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
        log.warn("Update chapter progress");
        ChapterProgress chapterProgress = chapterProgressRepository.findById(chapterProgressId)
                .orElseThrow(() -> new EntityNotFoundException("ChapterProgress not found"));

        if (chapterProgress.getProgressStatus().equals(ProgressStatus.COMPLETED)) {
            return;
        }

        chapterProgress.setCompletedAt(LocalDate.now());
        chapterProgress.setProgressStatus(ProgressStatus.COMPLETED);

        PlayerProgress playerProgress = playerProgressRepository.findById(chapterProgress.getPlayerProgress().getId())
                .orElseThrow(() -> new EntityNotFoundException("PlayerProgress not found"));
        log.warn(String.valueOf(playerProgress.getCompletedChapters()));
        playerProgress.setCompletedChapters(playerProgress.getCompletedChapters() + 1);
        log.warn(String.valueOf(playerProgress.getCompletedChapters()));

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
