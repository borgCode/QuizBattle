package org.borg.backend.achievement.repository;

import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.model.UserUnlockedAchievement;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserUnlockedAchievementRepository extends JpaRepository<UserUnlockedAchievement, Long> {
    boolean existsByPlayerAndAchievement(Player player, Achievement achievement);
    
    UserUnlockedAchievement findByPlayerAndAchievement(Player player, Achievement achievement);
}
