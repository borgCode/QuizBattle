package org.borg.backend.game.multiplayer.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;
import org.borg.backend.player.model.Player;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "session_players", indexes = {
        @Index(name = "IX_session_player", columnList = "session_id, player_id")
})
public class SessionPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private MultiplayerSession session;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;
    
    private Integer score;
    private Integer questionsAnswered;
    private boolean hasAcknowledgedGameOver;
    private boolean givenUp;

    @ElementCollection
    @CollectionTable(name = "player_question_results")
    private Set<PlayerQuestionResult> questionResults = new HashSet<>();
}
