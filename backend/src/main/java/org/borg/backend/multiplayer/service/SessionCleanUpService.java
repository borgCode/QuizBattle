package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionCleanUpService {

    private final PendingSessionRepository pendingSessionRepository;

    @Scheduled(fixedRate = 5000)
    public void cleanUpPendingSessions() {
        pendingSessionRepository.deleteOlderThan(Instant.now().minusMillis(15000));
    }

    public void cleanUpPendingSessions(Instant now) {
        pendingSessionRepository.deleteOlderThan(now.minusMillis(15000));
    }
}
