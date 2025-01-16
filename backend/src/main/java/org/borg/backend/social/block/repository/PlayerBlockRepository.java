package org.borg.backend.social.block.repository;

import org.borg.backend.player.model.Player;
import org.borg.backend.social.block.model.PlayerBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerBlockRepository extends JpaRepository<PlayerBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT DISTINCT p.blocked FROM PlayerBlock p WHERE p.blocker.id = :playerId")
    List<Player> getBlockedPlayers(Long playerId);


    @Query("SELECT CASE WHEN EXISTS (" +
            "  SELECT 1 FROM PlayerBlock pb " +
            "  WHERE (pb.blocker.id = :player1Id AND pb.blocked.id = :player2Id) " +
            "     OR (pb.blocker.id = :player2Id AND pb.blocked.id = :player1Id)" +
            ") THEN true ELSE false END")
    boolean checkIfBlockExists(Long player1Id, Long player2Id);
}
