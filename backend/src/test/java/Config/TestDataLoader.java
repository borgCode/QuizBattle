package Config;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private final RoleRepository roleRepository;
    private final PlayerRepository playerRepository;

    public TestDataLoader(QuestionRepository questionRepository, ChapterRepository chapterRepository, StoryRepository storyRepository, RoleRepository roleRepository, PlayerRepository playerRepository) {
        this.questionRepository = questionRepository;
        this.chapterRepository = chapterRepository;
        this.storyRepository = storyRepository;
        this.roleRepository = roleRepository;
        this.playerRepository = playerRepository;
    }
    
    public Player createTestPlayer() {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username("testPlayer")
                .password("password")
                .displayName("Test Player ")
                .accountLocked(false)
                .enabled(true)
                .roles(new ArrayList<>(List.of(userRole)))
                .build();

        return playerRepository.save(player);
        
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

    public void createTestStories(int numberOfStories, String[] categories) {
        
        for (int i = 0; i < numberOfStories; i++) {
            Story story = Story.builder()
                    .title("Test Story " + (i + 1))
                    .description("Test Story Description " + (i + 1))
                    .imagePath("test-story-image.jpg")
                    .numOfChapters(2) 
                    .build();

            Story savedStory = storyRepository.save(story);
            
            createChaptersForStory(savedStory, categories);
        }
    }

    private void createChaptersForStory(Story story, String[] categories) {
        for (int i = 0; i < story.getNumOfChapters(); i++) {
            Chapter chapter = Chapter.builder()
                    .story(story)
                    .chapterNumber(i + 1)
                    .title("Test Chapter " + (i + 1) + " for " + story.getTitle())
                    .description("Test Description for Chapter " + (i + 1))
                    .categories(new HashSet<>(Arrays.asList(categories)))
                    .imagePath("test-chapter-image.jpg")
                    .roundWinCondition(2)
                    .build();

            chapterRepository.save(chapter);
            createTestQuestionsForCategories(categories);
        }
    }

    public void cleanup() {
        questionRepository.deleteAll();
        chapterRepository.deleteAll();
        storyRepository.deleteAll();
    }


}
