package org.borg.backend.social.block.repository;

import org.borg.backend.social.block.model.PlayerBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerBlockRepository extends JpaRepository<PlayerBlock, Long> {
}
