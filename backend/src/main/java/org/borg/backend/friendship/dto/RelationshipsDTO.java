package org.borg.backend.friendship.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.dto.PlayerDTO;

import java.util.List;

@Getter
@Setter
@Builder
public class RelationshipsDTO {
    private List<PlayerDTO> friends;
    private List<PlayerDTO> blocked;
}
