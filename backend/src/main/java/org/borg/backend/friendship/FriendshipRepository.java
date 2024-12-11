package org.borg.backend.friendship;

import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByPlayer1AndPlayer2OrPlayer1AndPlayer2(Player player1A, Player player2A, Player player2B, Player player1B);
    
    Optional<Friendship> findByPlayer1AndPlayer2(Player player1, Player player2);

    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN Friendship f ON ((f.player1.id = :playerId AND p = f.player2) " +
            "OR (f.player2.id = :playerId AND p = f.player1))" +
            "AND f.status = :status")
    List<Player> getAllByPlayerId(Long playerId, FriendshipStatus status);
}
