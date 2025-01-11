package org.borg.backend.game.singleplayer.repository;

import org.borg.backend.game.singleplayer.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    
}
