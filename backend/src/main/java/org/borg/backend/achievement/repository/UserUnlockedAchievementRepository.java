package org.borg.backend.achievement.repository;

import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.model.UserUnlockedAchievement;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserUnlockedAchievementRepository extends JpaRepository<UserUnlockedAchievement, Long> {
    boolean existsByPlayerAndAchievement(Player player, Achievement achievement);
    
    UserUnlockedAchievement findByPlayerAndAchievement(Player player, Achievement achievement);

    @Query("SELECT ua FROM UserUnlockedAchievement ua WHERE ua.player.id = :playerId")
    List<UserUnlockedAchievement> findAllByPlayerId(@Param("playerId") Long playerId);
}
