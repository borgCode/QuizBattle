package org.borg.backend.game.multiplayer.repository;

import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultiplayerSessionRepository extends JpaRepository<MultiplayerSession, Long> {
    @Query("SELECT session FROM MultiplayerSession session " +
            "JOIN session.players player " +
            "WHERE player.id = :playerId")
    List<MultiplayerSession> findByPlayerId(@Param("playerId") Long playerId);


    @Query("SELECT COUNT(ms) > 0 FROM MultiplayerSession ms " +
            "WHERE EXISTS (SELECT 1 FROM ms.players player WHERE player.id = :player1Id) " +
            "AND EXISTS (SELECT 1 FROM ms.players player WHERE player.id = :player2Id) " +
            "AND ms.status = :gameStatus")
    boolean checkIfOngoingSessionExists(
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id,
            @Param("gameStatus") GameStatus gameStatus);
}
