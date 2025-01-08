package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlayerConversationDTO {
    private Long id;
    private String displayName;
    private String base64Image;
}
