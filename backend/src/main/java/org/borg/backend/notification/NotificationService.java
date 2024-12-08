package org.borg.backend.notification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getPlayerNotifications(Long playerId) {
        List<Notification> notifications = notificationRepository.findByPlayerIdAndIsReadFalse(playerId);
        log.warn("Notification list size is {}", notifications.size());
        return notifications;
    }
}
