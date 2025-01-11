package org.borg.backend.player.listener;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.service.StatsService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static org.borg.backend.player.events.StatsEvents.QuestionAnsweredEvent;

@Component
@RequiredArgsConstructor
public class StatsListener {

    private final StatsService statsService;

    @EventListener
    @Async
    public void handleQuestionEvent(QuestionAnsweredEvent event) {
        statsService.handleQuestionStats(event.playerId(), event.category(), event.isCorrect());
    }
}
