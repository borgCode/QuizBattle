package org.borg.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long playerId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private Long pendingSessionId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    
}

