package org.borg.backend.game.singleplayer.mapper;

import org.borg.backend.game.singleplayer.dto.StoryDTO;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface StoryMapper {
    
    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(story.getImagePath()))")
    StoryDTO toDto(Story story);
    List<StoryDTO> multipleToDto(List<Story> stories);
    
}
