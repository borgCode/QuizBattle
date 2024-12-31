package org.borg.backend.achievement.repository;

import org.borg.backend.achievement.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Achievement findByName(String name);
}
