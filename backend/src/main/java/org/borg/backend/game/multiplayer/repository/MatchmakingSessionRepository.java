package org.borg.backend.game.multiplayer.repository;

import org.borg.backend.game.multiplayer.model.MatchmakingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface MatchmakingSessionRepository extends JpaRepository<MatchmakingSession, Long> {

    
    @Query("select ms from MatchmakingSession ms " +
            "where (ms.player1Id = :player1Id and ms.player2Id = :player2Id) " +
            "OR (ms.player1Id = :player2Id and ms.player2Id = :player1Id)")
    MatchmakingSession findMatchmakingSessionByPlayerIds(@Param("player1Id") Long player1Id, @Param("player2Id") Long player2Id);

    @Modifying
    @Transactional
    @Query("DELETE FROM MatchmakingSession ps WHERE ps.createdAt < :instant")
    void deleteOlderThan(@Param("instant") Instant instant);
}
