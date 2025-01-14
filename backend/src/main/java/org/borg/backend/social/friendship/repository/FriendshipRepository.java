package org.borg.backend.social.friendship.repository;

import org.borg.backend.social.friendship.model.Friendship;
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
            "JOIN Friendship f ON (f.player1.id = :playerId AND p = f.player2 OR " +
            "f.player2.id = :playerId AND p = f.player1) " +
            "WHERE f.status = 'ACTIVE'")
    List<Player> getActiveFriends(Long playerId);

    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN Friendship f ON (f.player1.id = :playerId AND p = f.player2) " +
            "WHERE f.status = 'BLOCKED'")
    List<Player> getBlockedPlayers(Long playerId);

    List<Friendship> findByPlayer1IdAndPlayer2IdOrPlayer1IdAndPlayer2Id(Long player1Id, Long player1Id1, Long player1Id2, Long player1Id3);

    void deleteByPlayer1AndPlayer2OrPlayer1AndPlayer2(Player player1, Player player2, Player player11, Player player21);

    boolean existsByPlayer1AndPlayer2OrPlayer1AndPlayer2(Player player1, Player player2, Player player11, Player player21);
}
