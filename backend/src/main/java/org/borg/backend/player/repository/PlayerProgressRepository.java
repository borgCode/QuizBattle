package org.borg.backend.player.repository;

import org.borg.backend.player.model.PlayerProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerProgressRepository extends JpaRepository<PlayerProgress, Long> {
    PlayerProgress findByPlayerIdAndStoryId(Long playerId, Long storyId);

    List<PlayerProgress> findAllByPlayerId(Long playerId);
}
