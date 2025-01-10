package org.borg.backend.chapter.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.dto.InitiateProgressRequest;
import org.borg.backend.chapter.dto.InitiateProgressResponse;
import org.borg.backend.chapter.mapper.ChapterMapper;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.model.ChapterProgress;
import org.borg.backend.chapter.repository.ChapterProgressRepository;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.shared.enums.ProgressStatus;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
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


    public ChapterDTO getChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
        return ChapterMapper.toDTO(chapter);
    }

    @Transactional
    public InitiateProgressResponse initiateProgress(InitiateProgressRequest request) {
        PlayerProgress playerProgress = playerProgressRepository.findById(request.getPlayerProgressId())
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));
        
        if (!playerProgress.getPlayer().getId().equals(request.getPlayerId())) {
            throw new AccessDeniedException("Not authorized to initiate chapter");
        }

        playerProgress.setStartedAt(LocalDate.now());
        playerProgress.setProgressStatus(ProgressStatus.IN_PROGRESS);
        playerProgress.setCurrentChapterId(request.getChapterId());

        playerProgress = playerProgressRepository.save(playerProgress);

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
        
        ChapterProgress chapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(request.getPlayerProgressId(), chapter.getId());

        if (chapterProgress == null) {
            log.warn("Creating new chapter progress");
            chapterProgress = ChapterProgress.builder()
                    .playerProgress(playerProgress)
                    .chapter(chapter)
                    .startedAt(LocalDate.now())
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .build();

            chapterProgress = chapterProgressRepository.save(chapterProgress);
        }
        
        return new InitiateProgressResponse(playerProgress.getId(), chapterProgress.getId());
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
