package org.borg.backend.social.block.repository;

import org.borg.backend.player.model.Player;
import org.borg.backend.social.block.model.PlayerBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerBlockRepository extends JpaRepository<PlayerBlock, Long> {

    @Query("SELECT DISTINCT p.blocked FROM PlayerBlock p WHERE p.blocker.id = :playerId")
    List<Player> getBlockedPlayers(Long playerId);
    
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(Long blockerId, Long blockedId, Long blockerId1, Long blockedId1);
}
