package org.borg.backend.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.notification.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController()
@RequestMapping("notification")
@RequiredArgsConstructor
@Tag(name = "Notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("{playerId}")
    public ResponseEntity<List<Notification>> getActivePlayerNotifications(@PathVariable Long playerId) {
        return ResponseEntity.ok(notificationService.getActivePlayerNotifications(playerId));
    }

    @GetMapping("/archived/{playerId}")
    public ResponseEntity<List<Notification>> getAllPlayerNotifications(@PathVariable Long playerId) {
        return ResponseEntity.ok(notificationService.getAllPlayerNotifications(playerId));
    }
    
    @PostMapping("/send/{playerId}")
    public ResponseEntity<Void> sendNotification(@PathVariable Long playerId) {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/archive/{notificationId}")
    public ResponseEntity<Void> archiveNotification(@PathVariable Long notificationId) {
        notificationService.archiveNotification(notificationId);
        return ResponseEntity.ok().build();
    }
}
