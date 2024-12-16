package org.borg.backend.story.mapper;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.common.util.ImageUtil;
import org.borg.backend.story.dto.StoryDTO;
import org.borg.backend.story.model.Story;


import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StoryMapper {

    public static StoryDTO toDTO(Story story) {
        if (story == null) {
            return null;
        }

        return StoryDTO.builder()
                .id(story.getId())
                .title(story.getTitle())
                .description(story.getDescription())
                .introText(story.getIntroText())
                .numOfChapters(story.getNumOfChapters())
                .base64Image(ImageUtil.encodeAvatarImageFileToBase64(story.getImagePath()))
                .build();
    }

    public static List<StoryDTO> multipleToDTO(List<Story> stories) {
        List<StoryDTO> dtoList = new ArrayList<>();
        for (Story story : stories) {
            StoryDTO storyDTO = StoryDTO.builder()
                    .id(story.getId())
                    .title(story.getTitle())
                    .description(story.getDescription())
                    .introText(story.getIntroText())
                    .numOfChapters(story.getNumOfChapters())
                    .base64Image(ImageUtil.encodeStoryImageToBase64(story.getImagePath()))
                    .build();
            dtoList.add(storyDTO);
            log.warn("Story DTO base64 " + storyDTO.getBase64Image());
        }
        

        return dtoList;
    }
}
