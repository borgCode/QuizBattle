package org.borg.backend.social.notification.repository;

import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    

    Notification findByRecipientIdAndPendingSessionId(Long playerId, Long pendingSessionId);

    long countByIdInAndRecipientIdNot(Collection<Long> ids, Long playerId);

    void deleteByRecipientIdAndSenderId(Long playerId, Long senderId);

    void deleteByRecipientIdAndSenderIdAndType(Long playerId, Long senderId, NotificationType notificationType);
    

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :playerId " +
            "AND n.isArchived = false " +
            "AND n.hiddenByBlock = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByPlayerIdAndIsArchivedFalse(@Param("playerId") Long playerId);


    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipientId = :playerId " +
            "AND n.isArchived = true " +
            "AND (:searchFilter IS NULL OR n.message LIKE %:searchFilter%) " +
            "AND (:timeFilterStart IS NULL OR n.createdAt >= :timeFilterStart) " +
            "AND (:typeFilter IS NULL OR n.type = :typeFilter)")
    Page<Notification> findArchivedNotifications(
            @Param("playerId") Long playerId,
            @Param("searchFilter") String searchFilter,
            @Param("timeFilterStart") Instant timeFilterStart,
            @Param("typeFilter") NotificationType typeFilter,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification WHERE hiddenByBlock AND createdAt < :threshold")
    void deleteHiddenByOlderThan(@Param("threshold") Instant threshold);

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
    @Query("UPDATE Notification n SET n.hiddenByBlock = true WHERE n.recipientId = :blockerId AND n.senderId = :blockedId")
    void setNotificationsToHidden(@Param("blockerId") Long blocker, @Param("blockedId") Long blocked);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.hiddenByBlock = false WHERE n.recipientId = :blockerId AND n.senderId = :blockedId")
    void restoreHiddenNotifications(@Param("blockerId") Long blocker, @Param("blockedId") Long blocked);
}
