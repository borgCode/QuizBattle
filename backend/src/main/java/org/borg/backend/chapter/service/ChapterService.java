package org.borg.backend.chapter.service;


import jakarta.persistence.EntityNotFoundException;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.springframework.stereotype.Service;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;

    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public Chapter getChapter(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
    }
}
