package org.borg.backend.notification.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    
    public List<Notification> getActivePlayerNotifications(Long playerId) {
        return notificationRepository.findByPlayerIdAndIsArchivedFalse(playerId);
    }
    
    public List<Notification> getAllPlayerNotifications(Long playerId) {
        return notificationRepository.findByPlayerId(playerId);
    }
    
    public void sendFriendRequestNotification(Long receiverId, Player sendingPlayer) {
        String message = sendingPlayer.getDisplayName() + " sent you a friend request!";
        buildAndSaveNotification(receiverId, sendingPlayer.getId(), NotificationType.FRIEND_REQUEST, message, null, null);
    }

    public void sendFriendAcceptedNotification(Long receiverId, Player sendingPlayer) {
        String message = sendingPlayer.getDisplayName() + " accepted your friend request!";
        buildAndSaveNotification(receiverId, sendingPlayer.getId(), NotificationType.FRIEND_ACCEPTED, message, null, null);
    }

    public void sendRematchStartedNotification(Long receivingId, String senderDisplayName, Long newSessionId) {
        String message = "Your rematch request against " + senderDisplayName + " was accepted!";
        buildAndSaveNotification(receivingId, null, NotificationType.REMATCH_ACCEPTED, message, newSessionId, null);
    }

    public void sendRematchRequestNotification(Long receivingId, Long senderId, String senderDisplayName, Long pendingSessionId ) {
        String message = senderDisplayName + " requested a rematch against you!";
        buildAndSaveNotification(receivingId, senderId, NotificationType.REMATCH_REQUEST, message, null, pendingSessionId);
    }

    public void sendRematchAcceptedNotification(Long playerToNotify, String playerDisplayName, Long notificationId, Long newSessionId) {
        notificationRepository.deleteById(notificationId);

        String message = playerDisplayName + " accepted your request for a rematch!";
        buildAndSaveNotification(playerToNotify, null, NotificationType.REMATCH_ACCEPTED, message, newSessionId, null);
    }

    public void sendRematchRejectedNotification(Long playerToNotify, String playerDisplayName, Long notificationId) {
        notificationRepository.deleteById(notificationId);

        String message = "Your rematch request against " + playerDisplayName + " was declined!";
        buildAndSaveNotification(playerToNotify, null, NotificationType.REMATCH_DECLINED, message, null, null);
    }

    public void sendGameWonNotification(MultiplayerSession session) {
        Player opponent = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getLoserId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found"));

        String message = "You won your match against " + opponent.getDisplayName() + "!";
        buildAndSaveNotification(session.getWinnerId(), null, NotificationType.GAME_WON, message, session.getId(), null);
    }

    public void sendGameLostNotification(MultiplayerSession session) {
        Player opponent = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getWinnerId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found"));

        String message = "You lost your match against " + opponent.getDisplayName() + "!";
        buildAndSaveNotification(session.getLoserId(), null, NotificationType.GAME_LOST, message, session.getId(), null);
    }

    public void sendTieNotifications(MultiplayerSession session) {
        Player player1 = session.getPlayers().get(0);
        Player player2 = session.getPlayers().get(1);

        String messagePlayer1 = "Your match against " + player2.getDisplayName() + " was tied!";
        buildAndSaveNotification(player1.getId(), null, NotificationType.GAME_TIED, messagePlayer1, session.getId(), null);

        String messagePlayer2 = "Your match against " + player1.getDisplayName() + " was tied!";
        buildAndSaveNotification(player2.getId(), null, NotificationType.GAME_TIED, messagePlayer2, session.getId(), null);

    }

    private void buildAndSaveNotification(Long receiverId, Long senderId, NotificationType notificationType, String message, Long startedSessionId, Long pendingSessionId) {
        notificationRepository.save(Notification.builder()
                .playerId(receiverId)
                .senderId(senderId)
                .type(notificationType)
                .message(message)
                .startedSessionId(startedSessionId)
                .pendingSessionId(pendingSessionId)
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build());
    }
    
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
    public void archiveNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setArchived(true);
        notificationRepository.save(notification);
    }

    public void deleteFriendRequestByPlayerIds(Long id, Long id1) {
        notificationRepository.deleteByPlayerIdAndSenderIdAndType(id, id1, NotificationType.FRIEND_REQUEST);
    }

    public void deleteMatchRequestNotification(Long playerId, Long pendingSessionId) {
        Notification notification = notificationRepository.findByPlayerIdAndPendingSessionId(playerId, pendingSessionId);
        if (notification != null) {
            notificationRepository.delete(notification);
        }
    }
}
