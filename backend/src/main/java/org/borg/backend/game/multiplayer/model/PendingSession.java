package org.borg.backend.game.multiplayer.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
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
