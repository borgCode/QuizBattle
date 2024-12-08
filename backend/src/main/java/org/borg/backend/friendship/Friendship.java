package org.borg.backend.friendship;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.player.Player;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
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
