package org.borg.backend.multiplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.player.Player;
import org.borg.backend.question.PlayerQuestionResult;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class MultiplayerSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany
    @JoinTable(
            name = "session_players",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<Player> players;
    
    private Integer currentQuestionIndex;
    
    @ElementCollection
    @CollectionTable(name = "player_scores")
    @MapKeyColumn(name = "player_id")
    @Column(name = "score")
    private Map<Long, Integer> score;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    
    @ManyToOne
    @JoinColumn(name = "current_player_id")
    private Player currentPlayerTurn;
    
    @ElementCollection
    @CollectionTable(name = "questions_answered")
    @MapKeyColumn(name = "player_id")
    @Column(name = "count")
    private Map<Long, Integer> questionsAnswered;
    
    @ElementCollection
    private List<Long> questionIds;
    
    @ElementCollection
    @CollectionTable(name = "player_question_results")
    private Set<PlayerQuestionResult> questionResults;
    
    @ElementCollection
    private Set<String> playedCategories;
    
    @ElementCollection
    private List<String> roundCategories;
    

    public MultiplayerSession(Player player1, Player player2, Player currentPlayerTurn) {
        players = new ArrayList<>(List.of(player1, player2));
        score = new HashMap<>(Map.of(player1.getId(), 0, player2.getId(), 0));
        status = GameStatus.ACTIVE;
        this.currentQuestionIndex = 0;
        this.currentPlayerTurn = currentPlayerTurn;
        questionsAnswered = new HashMap<>(Map.of(player1.getId(), 0, player2.getId(), 0));
    }
}
