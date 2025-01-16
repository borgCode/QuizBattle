package org.borg.backend.social.chat.repository;

import org.borg.backend.social.chat.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    long countByIdInAndReceiverIdNot(Collection<Long> ids, Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.read = true " +
            "WHERE m.id IN :ids")
    void markMessagesAsRead(@Param("ids") List<Long> messageIds);
}
