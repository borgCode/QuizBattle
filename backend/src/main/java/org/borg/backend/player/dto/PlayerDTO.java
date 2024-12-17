package org.borg.backend.player.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.model.Stats;

@Getter
@Setter
@Builder
public class PlayerDTO {
    private Long id;
    private String username;
    private String displayName;
    private Stats stats;
    private String base64Image;
}
