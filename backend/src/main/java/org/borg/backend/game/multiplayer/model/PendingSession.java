package org.borg.backend.game.multiplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "IX_requestingId_opponentId", columnList = "requesting_player_id, opponent_id")
})
public class PendingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long requestingPlayerId;
    private Long opponentId;
    private Instant createdAt;
    private boolean requestingPlayerAccepted;
    private boolean opponentAccepted;


    public PendingSession(Long requestingPlayerId, Long opponentId) {
        this.requestingPlayerId = requestingPlayerId;
        this.opponentId = opponentId;
        this.createdAt = Instant.now();
        this.requestingPlayerAccepted = false;
        this.opponentAccepted = false;
    }
}
