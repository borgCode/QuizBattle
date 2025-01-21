package org.borg.backend.player.repository;

import org.borg.backend.player.model.StoryProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryProgressRepository extends JpaRepository<StoryProgress, Long> {
    StoryProgress findByPlayerIdAndStoryId(Long playerId, Long storyId);

    List<StoryProgress> findAllByPlayerId(Long playerId);
}
