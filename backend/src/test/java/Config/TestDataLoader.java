package Config;

import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Component
@TestComponent
public class TestDataLoader {
    private final QuestionRepository questionRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    
    private static final int CHAPTER_QUESTIONS_PER_CATEGORY = 5;

    public TestDataLoader(QuestionRepository questionRepository, ChapterRepository chapterRepository, StoryRepository storyRepository) {
        this.questionRepository = questionRepository;
        this.chapterRepository = chapterRepository;
        this.storyRepository = storyRepository;
    }

    public Chapter createTestChapter(String[] categories) {
        Story story = createTestStory();
        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(1)
                .title("Test Chapter")
                .description("Test Description")
                .categories(new HashSet<>(Arrays.asList(categories)))
                .roundWinCondition(2)
                .build();

        createTestQuestionsForCategories(categories);
        return chapterRepository.save(chapter);
    }

    private void createTestQuestionsForCategories(String[] categories) {
        for (String category : categories) {
            for (int i = 0; i < CHAPTER_QUESTIONS_PER_CATEGORY; i++) {
                Question question = Question.builder()
                        .category(category)
                        .question("Test Question " + i + " for " + category)
                        .options(List.of("Option 1", "Option 2", "Option 3", "Option 4"))
                        .correctAnswer("Option 1")
                        .build();
                questionRepository.save(question);
            }
        }
    }

    private Story createTestStory() {
        Story story = Story.builder()
                .title("Test Story")
                .description("Test Story Description")
                .numOfChapters(1)
                .build();
        return storyRepository.save(story);
    }

    public void cleanup() {
        questionRepository.deleteAll();
        chapterRepository.deleteAll();
        storyRepository.deleteAll();
    }
    
}
