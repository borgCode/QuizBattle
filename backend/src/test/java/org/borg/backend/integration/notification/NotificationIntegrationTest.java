package org.borg.backend.integration.notification;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.social.block.repository.PlayerBlockRepository;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.borg.backend.social.notification.service.NotificationCleanUpService;
import org.borg.backend.social.notification.service.NotificationService;
import org.borg.backend.player.model.Player;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationCleanUpService notificationCleanUpService;
    @Autowired
    private PlayerBlockService playerBlockService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PlayerBlockRepository playerBlockRepository;
    
    Player sendingPlayer;
    private final Long playerWithNotificationsId = 1L;

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

        notificationService.markAsRead(notificationList.get(0).getId(), playerWithNotificationsId);

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
        notificationService.markAllAsRead(notificationIds, playerWithNotificationsId);

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
        notificationService.archiveNotification(notificationList.get(0).getId(), playerWithNotificationsId);

        List<Notification> updatedNotificationsList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(0, updatedNotificationsList.size(), "Notification should be marked as archived");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> archivedNotifications = notificationService.getArchivedNotifications(playerWithNotificationsId, pageable, null, null);
        assertEquals(1, archivedNotifications.getContent().size(), "Notification should be marked as archived");
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


        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> archivedNotifications = notificationService.getArchivedNotifications(playerWithNotificationsId, pageable, null, null);
        assertEquals(2, archivedNotifications.getContent().size(), "There should be 1 archived notifications");

        Instant laterTime = Instant.now().plus(Duration.ofHours(49));
        notificationCleanUpService.archiveNotifications(laterTime);

        List<Notification> laterActiveNotifications = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
        assertEquals(0, laterActiveNotifications.size(),
                "There should be no active notifications after 49 hours");
    }

    @Nested
    class hiddenNotificationsTests {
        Player player1;
        Player player2;

        @BeforeEach
        void setUp() {
            playerRepository.deleteAll();
            if (roleRepository.findByName("USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepository.save(userRole);

                player1 = createAndSavePlayer("receiver");
                player2 = createAndSavePlayer("sender");
            }
        }

        private Player createAndSavePlayer(String playerName) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

            Player player = Player.builder()
                    .username(playerName.toLowerCase())
                    .password("password")
                    .displayName(playerName)
                    .accountLocked(false)
                    .enabled(true)
                    .roles(new ArrayList<>(List.of(userRole)))
                    .build();

            return playerRepository.save(player);
        }

        @AfterEach
        void tearDown() {
            playerBlockRepository.deleteAll();
            playerRepository.deleteAll();
            roleRepository.deleteAll();
        }

        @Test
        void shouldHideNotifications_WhenPlayerBlocks() {
            Long receiverId = player1.getId();

            notificationService.sendFriendRequestNotification(receiverId, player2, false);
            notificationService.sendGameWonNotification(receiverId, player2.getDisplayName(), 1L);
            notificationService.sendGameLostNotification(receiverId, player2.getDisplayName(), 2L);

            List<Notification> initialNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(receiverId);
            assertFalse(initialNotifications.isEmpty(), "Should have notifications before block");
            assertFalse(initialNotifications.stream().anyMatch(Notification::isHiddenByBlock),
                    "No notifications should be hidden initially");

            playerBlockService.blockPlayer(receiverId, player2.getId());

            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        List<Notification> hiddenNotifications = notificationRepository.findByPlayerIdAndIsArchivedFalse(player2.getId());
                        assertTrue(hiddenNotifications.stream().allMatch(Notification::isHiddenByBlock),
                                "All notifications should be hidden after block");
                    });

            Instant futureTime = Instant.now().plus(Duration.ofDays(15));
            notificationCleanUpService.deleteHiddenNotifications(futureTime);
        }

        @Test
        void shouldDeleteHiddenNotifications_WhenOlderThan14Days() {
            Notification oldNotification = Notification.builder()
                    .recipientId(player1.getId())
                    .senderId(player2.getId())
                    .type(NotificationType.FRIEND_REQUEST)
                    .message("Old notification")
                    .hiddenByBlock(true)
                    .createdAt(Instant.now().minus(Duration.ofDays(15)))
                    .build();
            notificationRepository.save(oldNotification);

            Notification recentNotification = Notification.builder()
                    .recipientId(player1.getId())
                    .senderId(player2.getId())
                    .type(NotificationType.FRIEND_REQUEST)
                    .message("Recent notification")
                    .hiddenByBlock(true)
                    .createdAt(Instant.now().minus(Duration.ofDays(13)))
                    .build();
            notificationRepository.save(recentNotification);

            notificationCleanUpService.deleteHiddenNotifications();

            List<Notification> remainingNotifications = notificationRepository.findAll();
            assertEquals(1, remainingNotifications.size(), "Should only have recent notification");
            assertEquals(recentNotification.getId(), remainingNotifications.get(0).getId(),
                    "Recent notification should still exist");
        }
    }

    @Nested
    class exceptionTests {

        @Test
        void shouldThrowErrorWhenArchiving_WhenNotMatchingPlayerId() {
            notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);

            List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> notificationService.archiveNotification(notificationList.get(0).getId(), sendingPlayer.getId()));

            assertEquals("Not authorized to mark notification as archived", exception.getMessage());
        }

        @Test
        void shouldThrowErrorWhenMarkingAsRead_WhenNotMatchingPlayerId() {
            notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);

            List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> notificationService.markAsRead(notificationList.get(0).getId(), sendingPlayer.getId()));

            assertEquals("Not authorized to mark notification as read", exception.getMessage());
        }

        @Test
        void shouldThrowErrorWhenMarkingAllAsRead_WhenNotMatchingPlayerId() {
            notificationService.sendFriendAcceptedNotification(playerWithNotificationsId, sendingPlayer);
            notificationService.sendGameWonNotification(playerWithNotificationsId, sendingPlayer.getDisplayName(), 999L);

            List<Notification> notificationList = notificationService.getActivePlayerNotifications(playerWithNotificationsId);
            List<Long> notificationIds = notificationList.stream()
                    .map(Notification::getId)
                    .toList();

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> notificationService.markAllAsRead(notificationIds, sendingPlayer.getId()));

            assertEquals("Not authorized to mark notifications as read", exception.getMessage());
        }
    }
}
