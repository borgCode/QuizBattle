package org.borg.backend.social.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.dto.PlayerConversationDTO;

import java.util.List;


@Getter
@Setter
@Builder
public class FullConversationDTO {
    private Long id;
    private PlayerConversationDTO otherPlayer;
    private List<MessageDTO> messages;  
    private boolean latestMessageIsRead;
}
