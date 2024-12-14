package org.borg.backend.notification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.model.NotificationType;
import org.borg.backend.player.model.Player;
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

    public void sendFriendRequestNotification(Long receiverId, Player sendingPlayer) {
        log.warn("Sending notification from {} to {}", receiverId, sendingPlayer.getDisplayName());
        notificationRepository.save(Notification.builder()
                .playerId(receiverId)
                .senderId(sendingPlayer.getId())
                .type(NotificationType.FRIEND_REQUEST)
                .message(sendingPlayer.getDisplayName() + " sent you a friend request!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendFriendAcceptedNotification(Long receiverId, Player sendingPlayer) {
        notificationRepository.save(Notification.builder()
                .playerId(receiverId)
                .type(NotificationType.FRIEND_ACCEPTED)
                .message(sendingPlayer.getDisplayName() + " accepted your friend request!")
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

    public void markAsRead(List<Long> notificationIds) {
        if (notificationIds.isEmpty()) {
            return;
        }
        notificationRepository.deleteAllById(notificationIds);
    }

    public void deleteFriendRequestByPlayerIds(Long id, Long id1) {
        notificationRepository.deleteByPlayerIdAndSenderIdAndType(id, id1, NotificationType.FRIEND_REQUEST);
    }
}
