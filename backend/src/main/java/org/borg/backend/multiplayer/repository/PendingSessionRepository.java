package org.borg.backend.multiplayer.repository;

import org.borg.backend.multiplayer.model.PendingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingSessionRepository extends JpaRepository<PendingSession, Long> {
    boolean existsByRequestingPlayerIdAndOpponentId(Long requestingPlayerId, Long opponentId);


    PendingSession findByRequestingPlayerIdAndOpponentId(Long requestingPlayerId, Long opponentId);
}
