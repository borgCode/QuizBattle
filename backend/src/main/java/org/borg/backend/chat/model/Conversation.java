package org.borg.backend.chat.model;


import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.player.model.Player;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "player1_id", nullable = false)
    private Long player1Id;
    
    @Column(name = "player2_id", nullable = false)
    private Long player2Id;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @OrderBy("sentAt DESC")
    private List<Message> messages = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "latest_message_id")
    private Message latestMessage;
}
