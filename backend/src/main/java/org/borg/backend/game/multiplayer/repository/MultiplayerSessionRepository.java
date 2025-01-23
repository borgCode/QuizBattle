package org.borg.backend.game.multiplayer.repository;

import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.shared.enums.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface MultiplayerSessionRepository extends JpaRepository<MultiplayerSession, Long> {
    
    
    @Query("SELECT session FROM MultiplayerSession session " +
            "JOIN session.sessionPlayers sp " +
            "WHERE sp.player.id = :playerId " +
            "ORDER BY session.status asc, session.lastUpdatedAt desc")
    Page<MultiplayerSession> findByPlayerId(@Param("playerId") Long playerId, Pageable pageable);


    @Query("SELECT COUNT(ms) > 0 FROM MultiplayerSession ms " +
            "WHERE EXISTS (SELECT 1 FROM ms.sessionPlayers sp WHERE sp.player.id = :player1Id) " +
            "AND EXISTS (SELECT 1 FROM ms.sessionPlayers sp WHERE sp.player.id = :player2Id) " +
            "AND ms.status = :gameStatus")
    boolean checkIfOngoingSessionExists(
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id,
            @Param("gameStatus") GameStatus gameStatus);
}
