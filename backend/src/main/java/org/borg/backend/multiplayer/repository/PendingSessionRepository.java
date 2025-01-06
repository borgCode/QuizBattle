package org.borg.backend.multiplayer.repository;

import org.borg.backend.multiplayer.model.PendingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface PendingSessionRepository extends JpaRepository<PendingSession, Long> {
    boolean existsByRequestingPlayerIdAndOpponentId(Long requestingPlayerId, Long opponentId);
    
    PendingSession findByRequestingPlayerIdAndOpponentId(Long requestingPlayerId, Long opponentId);
    
}
