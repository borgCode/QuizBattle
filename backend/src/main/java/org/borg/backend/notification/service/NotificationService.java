package org.borg.backend.notification.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getPlayerNotifications(Long playerId) {
        return notificationRepository.findByPlayerIdAndIsReadFalse(playerId);
    }

    public void sendFriendRequestNotification(Long receiverId, Player sendingPlayer) {
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

    public void sendRematchStartedNotification(Long receivingId, String senderDisplayName, Long newSessionId) {
        notificationRepository.save(Notification.builder()
                .playerId(receivingId)
                .type(NotificationType.REMATCH_ACCEPTED)
                .startedSessionId(newSessionId)
                .message("Your rematch request against " + senderDisplayName + " was accepted!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendRematchRequestNotification(
            Long receivingId, Long pendingSessionId, String senderDisplayName, Long senderId) {
        notificationRepository.save(Notification.builder()
                .playerId(receivingId)
                .senderId(senderId)
                .type(NotificationType.REMATCH_REQUEST)
                .message(senderDisplayName + " requested a rematch against you!")
                .pendingSessionId(pendingSessionId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void markAsRead(Long notificationId) {
        log.warn("Marking as read");
        notificationRepository.deleteById(notificationId);
    }

    public void deleteFriendRequestByPlayerIds(Long id, Long id1) {
        notificationRepository.deleteByPlayerIdAndSenderIdAndType(id, id1, NotificationType.FRIEND_REQUEST);
    }

    public void sendRematchAcceptedNotification(Long playerToNotify, String playerDisplayName, Long notificationId, Long newSessionId) {
        notificationRepository.deleteById(notificationId);

        notificationRepository.save(Notification.builder()
                .playerId(playerToNotify)
                .type(NotificationType.REMATCH_ACCEPTED)
                .startedSessionId(newSessionId)
                .message(playerDisplayName + " accepted your request for a rematch!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendRematchRejectedNotification(Long playerToNotify, String playerDisplayName, Long notificationId) {
        notificationRepository.deleteById(notificationId);

        notificationRepository.save(Notification.builder()
                .playerId(playerToNotify)
                .type(NotificationType.REMATCH_DECLINED)
                .message("Your rematch request against " + playerDisplayName + " was declined!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendTieNotifications(MultiplayerSession session) {
        Player player1 = session.getPlayers().get(0);
        Player player2 = session.getPlayers().get(1);

        List<Notification> notifications = new ArrayList<>();

        notifications.add(Notification.builder()
                .playerId(player1.getId())
                .type(NotificationType.GAME_TIED)
                .message("Your match against " + player2.getDisplayName() + " was tied!")
                .startedSessionId(session.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        notifications.add(Notification.builder()
                .playerId(player2.getId())
                .type(NotificationType.GAME_TIED)
                .message("Your match against " + player1.getDisplayName() + " was tied!")
                .startedSessionId(session.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        notificationRepository.saveAll(notifications);
    }

    public void sendGameWonNotification(MultiplayerSession session) {
        Player opponent = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getLoserId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found"));


        notificationRepository.save(Notification.builder()
                .playerId(session.getWinnerId())
                .type(NotificationType.GAME_WON)
                .message("You won your match against " + opponent.getDisplayName() + "!")
                .startedSessionId(session.getId())
                .opponentId(opponent.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void sendGameLostNotification(MultiplayerSession session) {
        Player opponent = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getWinnerId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found"));


        notificationRepository.save(Notification.builder()
                .playerId(session.getLoserId())
                .type(NotificationType.GAME_LOST)
                .message("You lost your match against " + opponent.getDisplayName() + "!")
                .startedSessionId(session.getId())
                .opponentId(opponent.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void deleteMatchRequestNotification(Long playerId, Long pendingSessionId) {
        Notification notification = notificationRepository.findByPlayerIdAndPendingSessionId(playerId, pendingSessionId);
        if (notification != null) {
            notificationRepository.delete(notification);
        }
    }
}
