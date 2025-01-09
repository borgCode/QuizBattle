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
        //TODO Du kan lika gärna lägga in hela build direkt in i dtoList.add(ChapterNoCategoriesDTO.builder()...);
        //TODO lambda forEach gör samma sak som for loop, men är dunder att använda och kortare.
         chapters.forEach(chapter -> dtoList.add(ChapterNoCategoriesDTO.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .description(chapter.getDescription())
            .unlockCondition(chapter.getUnlockCondition())
            .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
            .build()));
        //TODO Sen kan man bryta ut det ännu mer om det önskas som exemplet under så har du en rad kod och mer läsbart
        chapters.forEach(chapter -> dtoList.add(createChapterNoCategoriesDTO(chapter)));

        return dtoList;
    }

    private static ChapterNoCategoriesDTO createChapterNoCategoriesDTO(Chapter chapter) {
        return ChapterNoCategoriesDTO.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .description(chapter.getDescription())
            .unlockCondition(chapter.getUnlockCondition())
            .base64Image(ImageUtil.encodeStoryImageToBase64(chapter.getImagePath()))
            .build();
    }
    //TODO något som är riktigt fiffigt och smidigt att använda är en mapper lib, MapStruct är riktigt grym på detta och du borde kolla upp det.
    //TODO Det är samma sak för metoden som jag skapade ovanför, man kan använda MapStruct istället för builder om man vill konvertera objekt till resurs klass.
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
