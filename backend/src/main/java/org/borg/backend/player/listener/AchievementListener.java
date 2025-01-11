package org.borg.backend.player.listener;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.service.AchievementService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.borg.backend.player.events.AchievementEvents.*;

@Component
@RequiredArgsConstructor
public class AchievementListener {

    private final AchievementService achievementService;
    
    @EventListener
    @Async
    public void handleStoryCompleted(StoryCompletedEvent event) {
        achievementService.handleStoryAchievement(event.playerId(), event.storyName());
    }

    @EventListener
    @Async
    public void handleCategoryCompleted(CategoryCompletedEvent event) {
        achievementService.handleCategoryAchievement(event.playerId(), event.category());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleGameWon(GameWonEvent event) {
        achievementService.handleVictoryAchievement(event.playerId());
    }
}
