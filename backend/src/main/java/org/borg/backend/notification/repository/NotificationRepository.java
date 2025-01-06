package org.borg.backend.notification.repository;

import org.borg.backend.notification.model.Notification;
import org.borg.backend.common.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    
    void deleteByPlayerIdAndSenderIdAndType(Long id, Long id1, NotificationType notificationType);

    Notification findByPlayerIdAndPendingSessionId(Long playerId, Long pendingSessionId);

    List<Notification> findByPlayerIdAndIsArchivedFalse(Long playerId);
}
