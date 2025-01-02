package org.borg.backend.multiplayer.repository;

import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultiplayerSessionRepository extends JpaRepository<MultiplayerSession, Long> {
    @Query("SELECT session FROM MultiplayerSession session JOIN session.players player WHERE player.id = :playerId")
    List<MultiplayerSession> findByPlayerId(@Param("playerId") Long playerId);


    @Query("SELECT COUNT(ms) > 0 FROM MultiplayerSession ms " +
    "WHERE :player1 MEMBER OF ms.players AND :player2 MEMBER OF ms.players " +
    "AND ms.status = :gameStatus")
    boolean checkIfOngoingSessionExists(
            @Param("player1") Player sendingPlayer,
            @Param("player2") Player opponentPlayer,
            @Param("gameStatus") GameStatus gameStatus);
}
