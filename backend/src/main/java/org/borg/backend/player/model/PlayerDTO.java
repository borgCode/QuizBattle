package org.borg.backend.player.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlayerDTO {
    private Long id;
    private String username;
    private String displayName;
    private Stats stats;
}
