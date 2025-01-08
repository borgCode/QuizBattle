package org.borg.backend.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.dto.PlayerConversationDTO;

@Getter
@Setter
@Builder
public class ConversationPreviewDTO {
    private Long id;
    private PlayerConversationDTO otherPlayer;
    private boolean latestMessageIsRead;
    private String latestMessage;
}
