package org.borg.backend.social.friendship.repository;

import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByPlayer1AndPlayer2(Player player1, Player player2);

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.player1 = :player1 AND f.player2 = :player2) " +
            "OR (f.player1 =: player2 AND f.player2 = :player1)")
    Optional<Friendship> findExistingFriendshipByPlayers(Player player1, Player player2);

    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN Friendship f ON (f.player1.id = :playerId AND p = f.player2 " +
            "OR f.player2.id = :playerId AND p = f.player1) " +
            "WHERE f.status = 'ACTIVE'")
    List<Player> getActiveFriends(Long playerId);

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.player1.id = :player1Id AND f.player2.id = :player2Id) " +
            "OR (f.player1.id = :player2Id AND f.player2.id = :player1Id)")
    Optional<Friendship> findFriendshipBetweenPlayerIds(Long player1Id, Long player2Id);

    @Query("DELETE FROM Friendship f " +
            "WHERE (f.player1 = :player1 AND f.player2 = :player2) " +
            "OR (f.player1 = :player2 AND f.player2 = :player1)")
    @Modifying
    void deleteFriendship(Player player1, Player player2);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
            "(f.player1 = :player1 AND f.player2 = :player2) OR " +
            "(f.player1 = :player2 AND f.player2 = :player1)")
    boolean existsFriendship(Player player1, Player player2);
}
