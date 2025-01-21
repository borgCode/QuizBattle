package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.StoryProgressDTO;
import org.borg.backend.player.model.StoryProgress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoryProgressMapper {
    StoryProgressDTO toDTO(StoryProgress storyProgress);
}
