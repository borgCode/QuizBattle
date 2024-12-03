package org.borg.backend.multiplayer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class MultiplayerSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ElementCollection
    private List<Long> playerIds;
    private Integer currentQuestionIndex;
    @ElementCollection
    private Map<Long, Integer> score;
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    private Long currentPlayerTurn;
    @ElementCollection
    private Map<Long, Integer> questionsAnswered;
    @ElementCollection
    private List<Long> questionIds;
    

    public MultiplayerSession(Long player1Id, Long player2Id, Long currentPlayerTurn) {
        playerIds = new ArrayList<>(List.of(player1Id, player2Id));
        score = new HashMap<>(Map.of(player1Id, 0, player2Id, 0));
        status = GameStatus.ACTIVE;
        this.currentPlayerTurn = currentPlayerTurn;
        questionsAnswered = new HashMap<>(Map.of(player1Id, 0, player2Id, 0));
    }
}
