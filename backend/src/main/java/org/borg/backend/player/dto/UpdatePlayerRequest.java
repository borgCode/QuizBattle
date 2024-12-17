package org.borg.backend.player.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.common.enums.UpdateField;

@Getter
@Setter
@AllArgsConstructor
public class UpdatePlayerRequest {
    private Long playerId;
    private UpdateField updateField;
    private String newDisplayName;
}
