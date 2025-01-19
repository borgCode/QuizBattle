package org.borg.backend.social.chat.model;

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
@Table(indexes = {
        @Index(name = "idx_player1_player2", columnList = "player1_id, player2_id"),
        @Index(name = "idx_player2_player1", columnList = "player2_id, player1_id")
})
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private Player player2;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "latest_message_id")
    private Message latestMessage;

    public Player getOtherPlayer(Long currentPlayerId) {
        return getPlayer1().getId().equals(currentPlayerId) ? getPlayer2() : getPlayer1();
    }

    public boolean isRead(Long currentPlayerId) {
        if (latestMessage == null) {
            return true;
        }
        return latestMessage.getSenderId().equals(currentPlayerId) || (latestMessage.getReceiverId().equals(currentPlayerId) && latestMessage.isRead());
    }
}
