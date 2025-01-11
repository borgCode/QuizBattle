package org.borg.backend.game.singleplayer.mapper;

import org.borg.backend.game.singleplayer.dto.ChapterDTO;
import org.borg.backend.game.singleplayer.dto.ChapterNoCategoriesDTO;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.shared.util.ImageUtil;


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
                .rewardText(chapter.getRewardText())
                .categories(chapter.getCategories())
                .roundWinCondition(chapter.getRoundWinCondition())
                .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
                .build();

    }
    
    
}
