package org.borg.backend.social.chat.repository;

import org.borg.backend.social.chat.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c " +
            "WHERE (c.player1.id =:sender_id AND c.player2.id = :receiver_id) " +
            "OR (c.player1.id = :receiver_id AND c.player2.id =:sender_id)")
    Conversation findByBothPlayerIds(@Param("sender_id") Long senderId, @Param("receiver_id") Long receiverId);

    @Query("SELECT c FROM Conversation c " +
            "WHERE c.player1.id = :player_id OR c.player2.id = :player_id")
    List<Conversation> findConversationsByPlayerId(@Param("player_id") Long playerId);

    @Query("SELECT c.latestMessage.read FROM Conversation c WHERE c.id = :conversationId")
    boolean isLatestMessageRead(@Param("conversationId") Long conversationId);
}
