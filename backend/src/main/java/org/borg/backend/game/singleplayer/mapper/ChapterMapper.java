package org.borg.backend.game.singleplayer.mapper;

import org.borg.backend.game.singleplayer.dto.ChapterDTO;
import org.borg.backend.game.singleplayer.dto.ChapterNoCategoriesDTO;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = {ImageUtil.class})
public interface ChapterMapper {

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))")
    ChapterDTO toDTO(Chapter chapter);

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))")
    ChapterNoCategoriesDTO chapterToChapterNoCategoriesDTO(Chapter chapter);  // Add this method

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))")
    List<ChapterNoCategoriesDTO> multipleToNoCategoriesDTO(List<Chapter> chapters);
}
