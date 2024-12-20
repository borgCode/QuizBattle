package org.borg.backend.chapter.repository;

import org.borg.backend.chapter.model.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    ChapterProgress findByPlayerProgressIdAndChapterId(Long playerProgressId, Long chapterId);
}
