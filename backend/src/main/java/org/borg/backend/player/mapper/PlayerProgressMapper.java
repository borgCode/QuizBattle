package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.PlayerProgressDTO;
import org.borg.backend.player.model.PlayerProgress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerProgressMapper {
    PlayerProgressDTO toDTO(PlayerProgress playerProgress);
}
