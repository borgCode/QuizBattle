package org.borg.backend.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UpdatePlayerRequest {
    private Long playerId;
    private UpdateField updateField;
    private String newUsername;
    private String newDisplayName;
}
