package org.borg.backend.game.multiplayer.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
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
