package org.borg.backend.chat.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.player.model.Player;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    
    private Instant sentAt;
    private boolean isRead;
    private String content;
}
