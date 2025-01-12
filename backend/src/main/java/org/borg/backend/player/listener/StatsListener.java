package org.borg.backend.player.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.game.shared.enums.GameResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.borg.backend.player.events.StatsEvents.GameCompletedEvent;
import static org.borg.backend.player.events.StatsEvents.QuestionAnsweredEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsListener {

    private final StatsService statsService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    @Async
    public void handleQuestionEvent(QuestionAnsweredEvent event) {
        statsService.handleQuestionStats(event.playerId(), event.category(), event.isCorrect());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleGameCompleted(GameCompletedEvent event) {
        try {
            statsService.handleGameStats(event.player1(), event.player2(), event.result());
            
            if (!event.result().equals(GameResult.TIE)) {
                Long winnerId = event.result().equals(GameResult.WIN_PLAYER1)
                        ? event.player1().getId()
                        : event.player2().getId();
                applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(winnerId));
            }
        } catch (Exception e) {
            log.error("Failed to process game stats and achievements", e);
        }
    }
}
