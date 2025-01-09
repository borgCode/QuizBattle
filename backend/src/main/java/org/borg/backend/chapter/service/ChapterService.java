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
import org.borg.backend.common.enums.ProgressStatus;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        //TODO Varför loggar du log.warn här? Det är väl inget som behöver uppmärksammas, funkar bra med bara log.info i så fall eller om det är för debug syfte, log.debug.
        log.warn("Request params:{}, {}, {}, {}", request.getChapterId(), request.getPlayerId(), request.getPlayerProgressId(), request.getStoryId());

        PlayerProgress playerProgress = playerProgressRepository.findById(request.getPlayerProgressId())
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));

        playerProgress.setStartedAt(LocalDate.now());
        playerProgress.setProgressStatus(ProgressStatus.IN_PROGRESS);
        playerProgress.setCurrentChapterId(request.getChapterId());

        playerProgress = playerProgressRepository.save(playerProgress);

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));


        ChapterProgress chapterProgress = chapterProgressRepository.findByPlayerProgressIdAndChapterId(request.getPlayerProgressId(), chapter.getId());
        //TODO Här kan du skippa en hel if om du skulle returnera Optional<ChapterProgress> som du har gjort tidigare med .orELse(...)
        if (chapterProgress == null) {
            //TODO Undvik att skapa loggar som har bara en mening, det kan inte kopplas till något och svårt att felsöka vad som har gått snett. Lägg in relevanta ids.
            log.warn("Creating new chapter progress");
            chapterProgress = ChapterProgress.builder()
                    .playerProgress(playerProgress)
                    .chapter(chapter)
                    .startedAt(LocalDate.now())
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .build();

            chapterProgress = chapterProgressRepository.save(chapterProgress);
        }
        //TODO igen, det är inget att varna om, log.warn används ofta mer till om det är något som behöver uppmärksammas och kan leda till fel eller har gått fel men inte kritiskt som log.error.
        log.warn("Chapter info: {}, {}, {}", chapterProgress.getChapter().getTitle(),
                chapterProgress.getPlayerProgress().getPlayer().getDisplayName(),
                chapterProgress.getStartedAt());

        return new InitiateProgressResponse(playerProgress.getId(), chapterProgress.getId());
    }

    @Transactional
    public void updateChapterProgress(Long chapterProgressId) {
        //TODO log.warn igen samt en mening bara, lägg in chapterProgressId in i loggen för spårbarhet.
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
        //TODO debug loggar?
        log.warn(String.valueOf(playerProgress.getCompletedChapters()));
        playerProgress.setCompletedChapters(playerProgress.getCompletedChapters() + 1);
        //TODO debug loggar?
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
