package org.borg.backend.notification.service;

import org.borg.backend.common.enums.NotificationType;
import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    Player sendingPlayer;
    Long playerWithNotificationsId = 1L;
    @Autowired
    private NotificationCleanUpService notificationCleanUpService;

    @BeforeEach
    void setUp() {
        sendingPlayer = Player.builder()
                .id(999L)
                .displayName("Sender")
                .build();
        
    }
    
    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
    }
    
    @Test
    void testMarkingAsRead() {
        notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);
        
        List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(1, notificationList.size(), "Player should have one notification");
        assertFalse(notificationList.get(0).isRead(), "Notification should be unread");
        
        notificationService.markAsRead(notificationList.get(0).getId());

        List<Notification> updatedNotificationsList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(1, notificationList.size(), "Player should have one notification");
        assertTrue(updatedNotificationsList.get(0).isRead(), "Notification should be marked as read");
    }
    @Test
    void markingAllAsRead() {
        notificationService.sendGameWonNotification(playerWithNotificationsId, sendingPlayer.getDisplayName(), 999L);
        notificationService.sendGameWonNotification(playerWithNotificationsId, sendingPlayer.getDisplayName(), 999L);
        notificationService.sendGameLostNotification(playerWithNotificationsId, sendingPlayer.getDisplayName(), 999L);
        notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);

        List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        List<Notification> readNotifications = notificationList.stream()
                        .filter(Notification::isRead)
                                .toList();
        assertEquals(0, readNotifications.size(), "Player should have 0 read notifications");
        
        List<Long> notificationIds = notificationList.stream()
                .map(Notification::getId)
                .toList();
        notificationService.markAllAsRead(notificationIds);

        List<Notification> updatedNotificationsList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        List<Notification> updatedReadNotifications = updatedNotificationsList.stream()
                .filter(Notification::isRead)
                .toList();
        assertEquals(4, updatedReadNotifications.size(), "Player should have 4 read notifications");
        
    }
    @Test
    void testMarkingAsArchived() {
        notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);

        List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        notificationService.archiveNotification(notificationList.get(0).getId());

        List<Notification> updatedNotificationsList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(0, updatedNotificationsList.size(), "Notification should be marked as archived");
        
        updatedNotificationsList = notificationService.getAllPlayerNotifications(playerWithNotificationsId);
        assertEquals(1, updatedNotificationsList.size(), "Notification should be marked as archived");
    }
    
    @Test
    void testScheduledArchiving() {
        notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);
        
        notificationService.sendGameWonNotification(playerWithNotificationsId, sendingPlayer.getDisplayName(), 999L);

        Instant futureTime = Instant.now().plus(Duration.ofHours(25));
        notificationCleanUpService.archiveNotifications(futureTime);

        List<Notification> activeNotifications = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertAll("Active notifications at 25 hours",
                () -> assertEquals(1, activeNotifications.size(),
                        "There should only be one active notification"),
                () -> assertEquals(NotificationType.GAME_WON, activeNotifications.get(0).getType(),
                        "Active notification should be GAME_WON")
        );
        
        List<Notification> allNotifications = notificationService.getAllPlayerNotifications(playerWithNotificationsId);
        assertEquals(2, allNotifications.size(), "There should be two notifications");
        
        Instant laterTime = Instant.now().plus(Duration.ofHours(49));
        notificationCleanUpService.archiveNotifications(laterTime);
        
        List<Notification> laterActiveNotifications = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(0, laterActiveNotifications.size(),
                "There should be no active notifications after 49 hours");
        
    }
}
