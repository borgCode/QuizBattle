package org.borg.backend.social.friendship.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.player.model.Player;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_player1_player2", columnList = "player1_id, player2_id"),
        @Index(name = "idx_player2_player1", columnList = "player2_id, player1_id")
})
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Player player1;

    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Player player2;
    
    private LocalDate friendShipDate;
    
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    public Friendship(Player player1, Player player2, LocalDate friendShipDate, FriendshipStatus status) {
        this.player1 = player1;
        this.player2 = player2;
        this.friendShipDate = friendShipDate;
        this.status = status;
    }
}
