package org.borg.backend.player.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.service.AchievementService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.borg.backend.player.events.AchievementEvents.*;

@Slf4j
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCategoryCompleted(CategoryCompletedEvent event) {
        log.debug("Starting async achievement handling for category {} and player {}",
                event.category(), event.playerId());
        achievementService.handleCategoryAchievement(event.playerId(), event.category());
    }

    @EventListener
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameWon(GameWonEvent event) {
        achievementService.handleVictoryAchievement(event.playerId());
    }
}
