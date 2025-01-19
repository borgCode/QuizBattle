package org.borg.backend.game.multiplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.player.model.Player;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "multiplayer_session", indexes = {
        @Index(name = "IX_status", columnList = "status")
})
public class MultiplayerSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<SessionPlayer> sessionPlayers;
    
    private Integer currentQuestionIndex;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    
    @ElementCollection
    private List<Long> questionIds = new ArrayList<>();
    
    @ElementCollection
    private Set<String> playedCategories = new HashSet<>();
    
    @ElementCollection
    private List<String> roundCategories = new ArrayList<>();
    
    private Long winnerId = null;
    private Long loserId = null;
    private Boolean isTie = null;
    
    private Long currentPlayerTurnId;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(insertable = false)
    private Instant lastUpdatedAt;
    
    

    public MultiplayerSession(Player player1, Player player2, Long currentPlayerTurnId) {
        this.sessionPlayers = new ArrayList<>();
        status = GameStatus.ACTIVE;
        this.currentQuestionIndex = 0;

        SessionPlayer sessionPlayer1 = buildSessionPlayer(player1);
        SessionPlayer sessionPlayer2 = buildSessionPlayer(player2);
        this.sessionPlayers.add(sessionPlayer1);
        this.sessionPlayers.add(sessionPlayer2);
        
        this.currentPlayerTurnId = currentPlayerTurnId;
    }
    

    private SessionPlayer buildSessionPlayer(Player player) {
        return SessionPlayer.builder()
                .session(this)
                .player(player)
                .score(0)
                .questionsAnswered(0)
                .hasAcknowledgedGameOver(false)
                .givenUp(false)
                .questionResults(new HashSet<>()).build();
    }

    public SessionPlayer getSessionPlayerByPlayerId(Long playerId) {
        return sessionPlayers.stream()
                .filter(sp -> sp.getPlayer().getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public SessionPlayer getOpponentSessionPlayerId(Long playerId) {
        return sessionPlayers.stream()
                .filter(sp -> !sp.getPlayer().getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public Long getPlayerWhoGaveUpId() {
        return sessionPlayers.stream()
                .filter(SessionPlayer::isGivenUp)
                .map(sp -> sp.getPlayer().getId())
                .findFirst()
                .orElse(null);
    }
}
