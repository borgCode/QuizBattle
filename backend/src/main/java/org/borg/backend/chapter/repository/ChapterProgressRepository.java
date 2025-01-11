package org.borg.backend.chapter.repository;

import org.borg.backend.chapter.model.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    ChapterProgress findByPlayerProgressIdAndChapterId(Long playerProgressId, Long chapterId);

    @Query("SELECT cp FROM ChapterProgress cp " +
            "JOIN cp.playerProgress pp " +
            "WHERE pp.player.id = :playerId " +
            "AND cp.chapter.id = :chapterId")
    ChapterProgress findByPlayerIdAndChapterId(
            @Param("playerId") Long playerId,
            @Param("chapterId") Long chapterId
    );
}
