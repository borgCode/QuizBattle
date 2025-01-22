package org.borg.backend.player.repository;

import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementProgress;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementProgressRepository extends JpaRepository<AchievementProgress, Long> {
    boolean existsByPlayerAndAchievement(Player player, Achievement achievement);

    AchievementProgress findByPlayerAndAchievement(Player player, Achievement achievement);

    List<AchievementProgress> findAllByPlayer(Player player);
}
