package org.borg.backend.multiplayer;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class MultiplayerGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ElementCollection
    private List<Long> playerIds;
    private Integer currentQuestionIndex;
    @ElementCollection
    private Map<Long, Integer> score;
    
}
