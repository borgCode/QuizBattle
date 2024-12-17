package org.borg.backend.chapter.mapper;

import org.borg.backend.chapter.dto.ChapterDTO;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.common.util.ImageUtil;


import java.util.ArrayList;
import java.util.List;

public class ChapterMapper {
    public static ChapterDTO toDTO(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        return ChapterDTO.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .unlockCondition(chapter.getUnlockCondition())
                .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
                .build();

    }

    public static List<ChapterDTO> multipleToDTO(List<Chapter> chapters) {
        List<ChapterDTO> dtoList = new ArrayList<>();
        for (Chapter chapter : chapters) {
            ChapterDTO chapterDTO = ChapterDTO.builder()
                    .id(chapter.getId())
                    .chapterNumber(chapter.getChapterNumber())
                    .title(chapter.getTitle())
                    .description(chapter.getDescription())
                    .unlockCondition(chapter.getUnlockCondition())
                    .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
                    .build();
            dtoList.add(chapterDTO);
        }

        return dtoList;
    }
}
