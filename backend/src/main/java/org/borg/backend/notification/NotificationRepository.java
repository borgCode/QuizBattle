package org.borg.backend.notification;

import org.borg.backend.notification.model.Notification;
import org.borg.backend.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByPlayerIdAndIsReadFalse(Long playerId);

    
    void deleteByPlayerIdAndSenderIdAndType(Long id, Long id1, NotificationType notificationType);

    Notification findByPlayerIdAndPendingSessionId(Long playerId, Long pendingSessionId);
}
