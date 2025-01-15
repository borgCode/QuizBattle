package org.borg.backend.game.singleplayer.mapper;

import org.borg.backend.game.singleplayer.dto.PlayChapterDTO;
import org.borg.backend.game.singleplayer.dto.ChapterOverviewDTO;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = {ImageUtil.class})
public interface ChapterMapper {
    
    PlayChapterDTO toPlayChapterDTO(Chapter chapter);

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))")
    ChapterOverviewDTO toChapterOverviewDTO(Chapter chapter);  // Add this method

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))")
    List<ChapterOverviewDTO> multipleToChapterOverviewDTO(List<Chapter> chapters);
}
