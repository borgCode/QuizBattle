package org.borg.backend.multiplayer.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private boolean requestingPlayerAccepted;
    private boolean opponentAccepted;


    public PendingSession(Long requestingPlayerId, Long opponentId) {
        this.requestingPlayerId = requestingPlayerId;
        this.opponentId = opponentId;
        this.requestingPlayerAccepted = false;
        this.opponentAccepted = false;
    }
}
