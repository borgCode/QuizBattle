package org.borg.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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
    private Long senderId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private Long pendingSessionId;
    private Long startedSessionId;
    private String message;
    private boolean isRead;
    private boolean isArchived;
    private Instant createdAt;
    
}

