package org.borg.backend.social.notification.repository;

import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    
    void deleteByPlayerIdAndSenderIdAndType(Long id, Long id1, NotificationType notificationType);

    Notification findByPlayerIdAndPendingSessionId(Long playerId, Long pendingSessionId);

    @Query("SELECT n FROM Notification n WHERE n.playerId = :playerId AND n.isArchived = false ORDER BY n.createdAt DESC")
    List<Notification> findByPlayerIdAndIsArchivedFalse(Long playerId);
    
    List<Notification> findByPlayerId(Long playerId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isArchived = true" +
            " WHERE n.type IN :types " +
            "AND n.createdAt < :threshold " +
            "AND n.isArchived = false")
    void archiveByTypeAndOlderThan(@Param("types") List<NotificationType> notificationTypes, @Param("threshold") Instant threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.id IN :notificationIds")
    void markNotificationsAsRead(@Param("notificationIds") List<Long> notificationIds);

    long countByIdInAndPlayerIdNot(Collection<Long> ids, Long playerId);
}
