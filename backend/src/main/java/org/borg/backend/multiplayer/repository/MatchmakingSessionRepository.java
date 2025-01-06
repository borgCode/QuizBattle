package org.borg.backend.multiplayer.repository;

import org.borg.backend.multiplayer.model.MatchmakingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface MatchmakingSessionRepository extends JpaRepository<MatchmakingSession, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MatchmakingSession  ps WHERE ps.createdAt < :instant")
    void deleteOlderThan(@Param("instant") Instant instant);

    MatchmakingSession findByRequestingPlayerIdAndOpponentIdOrRequestingPlayerIdAndOpponentId(Long requestingPlayerId, Long requestingPlayerId1, Long requestingPlayerId2, Long requestingPlayerId3);
}
