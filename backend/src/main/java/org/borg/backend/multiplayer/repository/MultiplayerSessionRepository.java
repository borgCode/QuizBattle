package org.borg.backend.multiplayer.repository;

import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultiplayerSessionRepository extends JpaRepository<MultiplayerSession, Long> {
    @Query("SELECT session FROM MultiplayerSession session JOIN session.players player WHERE player.id = :playerId")
    List<MultiplayerSession> findByPlayerId(@Param("playerId") Long playerId);
}
