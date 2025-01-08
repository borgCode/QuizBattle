package org.borg.backend.chat.repository;

import org.borg.backend.chat.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query ("SELECT c FROM Conversation c " +
            "WHERE c.player1.id = :player_id OR c.player2.id = :player_id")
    List<Conversation> findConversationsByPlayerId(@Param("player_id") Long playerId);
}
