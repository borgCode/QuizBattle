package org.borg.backend.social.block.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.player.model.Player;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PlayerBlock {
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Player blocker;
    
    @ManyToOne
    private Player blocked;
    
    private Instant blockDate;
    
}
