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


    List<Notification> findByPlayerId(Long playerId);

    Notification findByPlayerIdAndPendingSessionId(Long playerId, Long pendingSessionId);

    long countByIdInAndPlayerIdNot(Collection<Long> ids, Long playerId);

    void deleteByPlayerIdAndSenderIdAndType(Long playerId, Long senderId, NotificationType notificationType);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification WHERE hiddenByBlock AND createdAt < :threshold")
    void deleteHiddenByOlderThan(@Param("threshold") Instant threshold);
    
    @Query("SELECT n FROM Notification n WHERE n.playerId = :playerId AND n.isArchived = false ORDER BY n.createdAt DESC")
    List<Notification> findByPlayerIdAndIsArchivedFalse(@Param("playerId") Long playerId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isArchived = true WHERE n.type IN :types AND n.createdAt < :threshold AND n.isArchived = false")
    void archiveByTypeAndOlderThan(@Param("types") List<NotificationType> notificationTypes, @Param("threshold") Instant threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :notificationIds")
    void markNotificationsAsRead(@Param("notificationIds") List<Long> notificationIds);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.hiddenByBlock = true WHERE n.playerId = :blockerId AND n.senderId = :blockedId")
    void setNotificationsToHidden(@Param("blockerId") Long blocker, @Param("blockedId") Long blocked);
}
