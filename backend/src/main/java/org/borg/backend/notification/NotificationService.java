package org.borg.backend.notification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.model.PendingSession;
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

    public void sendFriendRequestNotification(Long receiverId, String senderDisplayName) {
        log.warn("Sending notification from {} to {}", receiverId, senderDisplayName);
        notificationRepository.save(Notification.builder()
                .playerId(receiverId)
                .type(NotificationType.FRIEND_REQUEST)
                .message(senderDisplayName + " sent you a friend request!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendRematchStartedNotification(Long receivingId, String senderDisplayName) {
        notificationRepository.save(Notification.builder()
                .playerId(receivingId)
                .type(NotificationType.REMATCH_ACCEPTED)
                .message("Your rematch request against " + senderDisplayName + " was accepted!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendRematchRequestNotification(
            Long receivingId, Long pendingSessionId, String senderDisplayName) {
        notificationRepository.save(Notification.builder()
                .playerId(receivingId)
                .type(NotificationType.REMATCH_REQUEST)
                .message(senderDisplayName + " request a rematch against you!")
                .pendingSessionId(pendingSessionId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
