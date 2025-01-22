package org.borg.backend.player.repository;

import org.borg.backend.player.model.Achievement;
import org.borg.backend.player.model.AchievementLevel;
import org.borg.backend.player.model.AchievementLevelHistory;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementLevelHistoryRepository extends JpaRepository<AchievementLevelHistory, Long> {
    boolean existsByPlayerAndAchievement(Player player, Achievement achievement);

    boolean existsByPlayerAndAchievementLevel(Player player, AchievementLevel achievementLevel);

    List<AchievementLevelHistory> findByPlayerAndAchievement(Player player, Achievement achievement);
}
