package org.borg.backend.notification.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.shared.enums.NotificationType;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.borg.backend.shared.enums.NotificationType.*;

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
        buildAndSaveNotification(receiverId, sendingPlayer.getId(), FRIEND_REQUEST, message, null, null);
    }

    public void sendFriendAcceptedNotification(Long receiverId, Player sendingPlayer) {
        String message = sendingPlayer.getDisplayName() + " accepted your friend request!";
        buildAndSaveNotification(receiverId, sendingPlayer.getId(), FRIEND_ACCEPTED, message, null, null);
    }

    public void sendMatchStartedNotification(Long receivingId, String senderDisplayName, Long newSessionId) {
        String message = "Your match request against " + senderDisplayName + " was accepted!";
        buildAndSaveNotification(receivingId, null, MATCH_ACCEPTED, message, newSessionId, null);
    }

    public void sendMatchRequestNotification(Long receivingId, Long senderId, String senderDisplayName, Long pendingSessionId) {
        String message = senderDisplayName + " requested a match against you!";
        buildAndSaveNotification(receivingId, senderId, MATCH_REQUEST, message, null, pendingSessionId);
    }
    
    public void sendMatchAcceptedNotification(Long playerToNotify, String playerDisplayName, Long notificationId, Long newSessionId) {
        notificationRepository.deleteById(notificationId);

        String message = playerDisplayName + " accepted your match request!";
        buildAndSaveNotification(playerToNotify, null, MATCH_ACCEPTED, message, newSessionId, null);
    }
    
    public void sendMatchRejectedNotification(Long playerToNotify, String playerDisplayName, Long notificationId) {
        notificationRepository.deleteById(notificationId);

        String message = "Your match request against " + playerDisplayName + " was declined!";
        buildAndSaveNotification(playerToNotify, null, MATCH_DECLINED, message, null, null);
    }

    public void sendRematchStartedNotification(Long receivingId, String senderDisplayName, Long newSessionId) {
        String message = "Your rematch request against " + senderDisplayName + " was accepted!";
        buildAndSaveNotification(receivingId, null, REMATCH_ACCEPTED, message, newSessionId, null);
    }

    public void sendRematchRequestNotification(Long receivingId, Long senderId, String senderDisplayName, Long pendingSessionId) {
        String message = senderDisplayName + " requested a rematch against you!";
        buildAndSaveNotification(receivingId, senderId, REMATCH_REQUEST, message, null, pendingSessionId);
    }

    public void sendRematchAcceptedNotification(Long playerToNotify, String playerDisplayName, Long notificationId, Long newSessionId) {
        notificationRepository.deleteById(notificationId);

        String message = playerDisplayName + " accepted your request for a rematch!";
        buildAndSaveNotification(playerToNotify, null, REMATCH_ACCEPTED, message, newSessionId, null);
    }

    public void sendRematchRejectedNotification(Long playerToNotify, String playerDisplayName, Long notificationId) {
        notificationRepository.deleteById(notificationId);

        String message = "Your rematch request against " + playerDisplayName + " was declined!";
        buildAndSaveNotification(playerToNotify, null, REMATCH_DECLINED, message, null, null);
    }

    public void sendGameWonNotification(Long winnerId, String opponentDisplayName, Long sessionId) {
        String message = "You won your match against " + opponentDisplayName + "!";
        buildAndSaveNotification(winnerId, null, GAME_WON, message, sessionId, null);
    }

    public void sendGameLostNotification(Long loserId, String opponentDisplayName, Long sessionId) {
        String message = "You lost your match against " + opponentDisplayName + "!";
        buildAndSaveNotification(loserId, null, GAME_LOST, message, sessionId, null);
    }

    public void sendTieNotifications(List<Player> players, Long sessionId) {
        Player player1 = players.get(0);
        Player player2 = players.get(1);
        
        String messagePlayer1 = "Your match against " + player1.getDisplayName() + " was tied!";
        buildAndSaveNotification(player1.getId(), null, GAME_TIED, messagePlayer1, sessionId, null);

        String messagePlayer2 = "Your match against " + player2.getDisplayName() + " was tied!";
        buildAndSaveNotification(player2.getId(), null, GAME_TIED, messagePlayer2, sessionId, null);

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
                .createdAt(Instant.now())
                .build());
    }
    
    public void markAsRead(long notificationId, long playerId) {
        Notification notification = notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        if (!notification.getPlayerId().equals(playerId)) {
            throw new AccessDeniedException("Not authorized to mark notification as read");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
    }
    public void markAllAsRead(List<Long> notificationIds, long playerId) {
        long invalidIdCount = notificationRepository.countByIdInAndPlayerIdNot(notificationIds, playerId);
        
        if (invalidIdCount > 0) {
            throw new AccessDeniedException("Not authorized to mark notifications as read");
        }
        
        notificationRepository.markNotificationsAsRead(notificationIds);
    }
    
    public void archiveNotification(long notificationId, long playerId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getPlayerId().equals(playerId)) {
            throw new AccessDeniedException("Not authorized to mark notification as archived");
        }
        
        notification.setArchived(true);
        notificationRepository.save(notification);
    }

    public void deleteFriendRequestByPlayerIds(Long id, Long id1) {
        notificationRepository.deleteByPlayerIdAndSenderIdAndType(id, id1, FRIEND_REQUEST);
    }

    public void deleteMatchRequestNotification(Long playerId, Long pendingSessionId) {
        Notification notification = notificationRepository.findByPlayerIdAndPendingSessionId(playerId, pendingSessionId);
        if (notification != null) {
            notificationRepository.delete(notification);
        }
    }
}
