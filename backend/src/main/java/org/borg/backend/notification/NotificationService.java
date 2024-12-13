package org.borg.backend.notification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getPlayerNotifications(Long playerId) {
        return notificationRepository.findByPlayerIdAndIsReadFalse(playerId);
    }

    public void sendFriendRequestNotification(Long id, String displayName) {
        log.warn("Sending notification from {} to {}", id, displayName);
        notificationRepository.save(Notification.builder()
                .playerId(id)
                .type(NotificationType.FRIEND_REQUEST)
                .message(displayName + " sent you a friend request!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
