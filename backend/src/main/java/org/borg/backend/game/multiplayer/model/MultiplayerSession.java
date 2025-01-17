package org.borg.backend.game.multiplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.player.model.Player;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class MultiplayerSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<SessionPlayer> sessionPlayers;
    
    private Integer currentQuestionIndex;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    
    @ManyToOne
    @JoinColumn(name = "current_player_id")
    private Player currentPlayerTurn;
    
    @ElementCollection
    private List<Long> questionIds = new ArrayList<>();
    
    @ElementCollection
    private Set<String> playedCategories = new HashSet<>();
    
    @ElementCollection
    private List<String> roundCategories = new ArrayList<>();
    
    private Long winnerId = null;
    private Long loserId = null;
    private Boolean isTie = null;
    
    

    public MultiplayerSession(Player player1, Player player2, Player currentPlayerTurn) {
        this.sessionPlayers = new ArrayList<>();
        status = GameStatus.ACTIVE;
        this.currentQuestionIndex = 0;
        this.currentPlayerTurn = currentPlayerTurn;
        
        this.sessionPlayers.add(buildSessionPlayer(player1));
        this.sessionPlayers.add(buildSessionPlayer(player2));
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

    public SessionPlayer getSessionPlayerById(Long playerId) {
        return sessionPlayers.stream()
                .filter(sp -> sp.getPlayer().getId().equals(playerId))
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
