package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.repository.MatchmakingSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionCleanUpService {

    private final MatchmakingSessionRepository matchmakingSessionRepository;

    @Scheduled(fixedRate = 5000)
    public void cleanUpMatchmakingSessions() {
        matchmakingSessionRepository.deleteOlderThan(Instant.now().minusMillis(15000));
    }

    public void cleanUpMatchmakingSessions(Instant now) {
        matchmakingSessionRepository.deleteOlderThan(now.minusMillis(15000));
    }
}
