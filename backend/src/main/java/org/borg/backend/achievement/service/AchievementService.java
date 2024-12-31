package org.borg.backend.achievement.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.borg.backend.achievement.model.UserUnlockedAchievement;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final PlayerRepository playerRepository;
    private final UserUnlockedAchievementRepository userUnlockedAchievementRepository;

    @EventListener
    @Async
    public void handleStoryCompleted(AchievementEvents.StoryCompletedEvent event) {

        Achievement achievement = achievementRepository.findByName(event.storyName());
        
        Player player = playerRepository.findById(event.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));
        
        if (userUnlockedAchievementRepository.existsByPlayerAndAchievement(player, achievement)) {
            //TODO error handling for already exists
            return;
        }

        UserUnlockedAchievement unlockedAchievement = UserUnlockedAchievement.builder()
                .player(player)
                .achievement(achievement)
                .level(achievement.getLevels().get(0))
                .achievedAt(LocalDateTime.now())
                .build();
        
        userUnlockedAchievementRepository.save(unlockedAchievement);
        
    }
}
