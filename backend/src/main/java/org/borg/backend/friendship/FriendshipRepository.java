package org.borg.backend.friendship;

import org.borg.backend.player.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByPlayer1AndPlayer2(Player player1, Player player2);

    @Query("SELECT CASE WHEN friendship.player1.id = :playerId THEN friendship.player2 ELSE friendship.player1 END " +
            "FROM Friendship friendship " +
            "WHERE friendship.player1.id = :playerId OR friendship.player2.id = :playerId")
    List<Player> getAllByPlayerId(Long playerId);
}
