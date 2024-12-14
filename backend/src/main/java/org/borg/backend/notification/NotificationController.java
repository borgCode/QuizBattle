package org.borg.backend.notification;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.borg.backend.notification.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("notification")
@RequiredArgsConstructor
@Tag(name = "Notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("{playerId}")
    public ResponseEntity<List<Notification>> getPlayerNotifications (@PathVariable Long playerId) {
        return ResponseEntity.ok(notificationService.getPlayerNotifications(playerId));
    }
    
    @PostMapping("/send/{playerId}")
    public ResponseEntity<Void> sendNotification(@PathVariable Long playerId) {
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(@RequestParam List<Long> notificationIds) {
        notificationService.markAsRead(notificationIds);
        return ResponseEntity.ok().build();
    }
}
