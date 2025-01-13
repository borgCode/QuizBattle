package org.borg.backend.social.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.social.notification.service.NotificationService;
import org.borg.backend.social.notification.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController()
@RequestMapping("notification")
@RequiredArgsConstructor
@Tag(name = "Notification")
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("{playerId}")
    public ResponseEntity<List<Notification>> getActivePlayerNotifications(@PathVariable long playerId) {
        return ResponseEntity.ok(notificationService.getActivePlayerNotifications(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/archived/{playerId}")
    public ResponseEntity<List<Notification>> getAllPlayerNotifications(@PathVariable long playerId) {
        return ResponseEntity.ok(notificationService.getAllPlayerNotifications(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable long notificationId, @RequestParam long playerId) {
        notificationService.markAsRead(notificationId, playerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/read/all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam List<Long> notificationIds, @RequestParam long playerId) {
        notificationService.markAllAsRead(notificationIds, playerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/archive/{notificationId}")
    public ResponseEntity<Void> archiveNotification(@PathVariable long notificationId, @RequestParam long playerId) {
        notificationService.archiveNotification(notificationId, playerId);
        return ResponseEntity.ok().build();
    }
}
