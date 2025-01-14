package org.borg.backend.social.notification.service;


import lombok.RequiredArgsConstructor;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationCleanUpService {
    
    private final NotificationRepository notificationRepository;
    
    @Scheduled(fixedRate = 10000)
    public void archiveNotifications() {
        
        notificationRepository.archiveByTypeAndOlderThan(
                List.of(NotificationType.FRIEND_ACCEPTED),
                Instant.now().minus(Duration.ofHours(24))
        );

        notificationRepository.archiveByTypeAndOlderThan(
                List.of(NotificationType.GAME_WON, NotificationType.GAME_LOST, NotificationType.GAME_TIED),
                Instant.now().minus(Duration.ofHours(48))
        );
        
    }

    public void archiveNotifications(Instant instant) {

        notificationRepository.archiveByTypeAndOlderThan(
                List.of(NotificationType.FRIEND_ACCEPTED),
                instant.minus(Duration.ofHours(24))
        );

        notificationRepository.archiveByTypeAndOlderThan(
                List.of(NotificationType.GAME_WON, NotificationType.GAME_LOST, NotificationType.GAME_TIED),
                instant.minus(Duration.ofHours(48))
        );
    }

    @Scheduled(fixedRate = 10000)
    public void deleteHiddenNotifications() {
        notificationRepository.deleteHiddenByOlderThan(Instant.now().minus(Duration.ofDays(14)));
    }
}
