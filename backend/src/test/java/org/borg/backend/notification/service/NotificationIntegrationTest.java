package org.borg.backend.notification.service;

import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.repository.NotificationRepository;
import org.borg.backend.player.model.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
        Long playerWithNotificationsId = 1L;
        
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
    void testMarkingAsArchived() {
        Long playerWithNotificationsId = 1L;

        notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);

        List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        notificationService.archiveNotification(notificationList.get(0).getId());

        List<Notification> updatedNotificationsList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(0, updatedNotificationsList.size(), "Notification should be marked as archived");
        
        updatedNotificationsList = notificationService.getAllPlayerNotifications(playerWithNotificationsId);
        assertEquals(1, updatedNotificationsList.size(), "Notification should be marked as archived");
    }
}
