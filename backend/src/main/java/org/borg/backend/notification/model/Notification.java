package org.borg.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.common.enums.NotificationType;

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
    private Long senderId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private Long pendingSessionId;
    private Long startedSessionId;
    private Long opponentId;
    private String message;
    private boolean isRead;
    private boolean isArchived;
    private LocalDateTime createdAt;
    
}

