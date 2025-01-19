package org.borg.backend.game.multiplayer.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "IX_player1_player2", columnList = "player1id, player2id"),
        @Index(name = "IX_player2_player1", columnList = "player2id, player1id"),
        @Index(name = "IX_createdAt", columnList = "created_at")
})
public class MatchmakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long player1Id;
    private Long player2Id;
    private Instant createdAt;
    private boolean player1Accepted;
    private boolean player2Accepted;


    public MatchmakingSession(Long player1Id, Long player2Id) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.createdAt = Instant.now();
        this.player1Accepted = false;
        this.player2Accepted = false;
    }
}
