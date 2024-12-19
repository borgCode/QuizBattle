package org.borg.backend.chapter.mapper;

import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.dto.ChapterNoCategoriesDTO;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.common.util.ImageUtil;


import java.util.ArrayList;
import java.util.List;

public class ChapterMapper {

    public static List<ChapterNoCategoriesDTO> multipleToNoCategoriesDTO(List<Chapter> chapters) {
        List<ChapterNoCategoriesDTO> dtoList = new ArrayList<>();
        for (Chapter chapter : chapters) {
            ChapterNoCategoriesDTO chapterNoCategoriesDTO = ChapterNoCategoriesDTO.builder()
                    .id(chapter.getId())
                    .chapterNumber(chapter.getChapterNumber())
                    .title(chapter.getTitle())
                    .description(chapter.getDescription())
                    .unlockCondition(chapter.getUnlockCondition())
                    .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
                    .build();
            dtoList.add(chapterNoCategoriesDTO);
        }

        return dtoList;
    }

    public static ChapterDTO toDTO(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        return ChapterDTO.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .categories(chapter.getCategories())
                .roundWinCondition(chapter.getRoundWinCondition())
                .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
                .build();

    }
    
    
}
