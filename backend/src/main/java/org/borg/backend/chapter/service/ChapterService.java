package org.borg.backend.chapter.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final ChapterProgressRepository chapterProgressRepository;


    public ChapterDTO getChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));;
        return ChapterMapper.toDTO(chapter);
    }

    @Transactional
    public InitiateProgressResponse initiateProgress(InitiateProgressRequest request) {
        log.warn("Request params:{}, {}, {}, {}", request.getChapterId(), request.getPlayerId(), request.getPlayerProgressId(), request.getStoryId());
        
        PlayerProgress playerProgress = playerProgressRepository.findById(request.getPlayerProgressId())
                .orElseThrow(() -> new EntityNotFoundException("Progress not found"));
        
        playerProgress.setStartedAt(LocalDate.now());
        playerProgress.setProgressStatus(ProgressStatus.IN_PROGRESS);
        
        playerProgress = playerProgressRepository.save(playerProgress);
        
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));

        ChapterProgress chapterProgress = ChapterProgress.builder()
                .playerProgress(playerProgress)
                .chapter(chapter)
                .startedAt(LocalDate.now())
                .progressStatus(ProgressStatus.IN_PROGRESS)
                .build();
                
        chapterProgress = chapterProgressRepository.save(chapterProgress);
        
        log.warn("Chapter info: {}, {}, {}", chapterProgress.getChapter().getTitle(),
                chapterProgress.getPlayerProgress().getPlayer().getDisplayName(),
                chapterProgress.getStartedAt());
        
        return new InitiateProgressResponse(playerProgress.getId(), chapterProgress.getId());
    }
}
