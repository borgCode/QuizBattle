package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.question.Question;
import org.borg.backend.question.QuestionRepository;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.chapter.ChapterRepository;
import org.borg.backend.story.repository.StoryRepository;
import org.borg.backend.chapter.Chapter;
import org.borg.backend.story.model.Story;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final PlayerRepository playerRepository;
    private final RoleRepository roleRepository;
    private final SeedService seedService;
    private final QuestionRepository questionRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;


    @Override
    public void run(String... args) {

//        List<Role> roles = createRoles();
//
//        initQuestions();
//
//        seedService.seedTestData();
//        initStories();
    }

    private List<Role> createRoles() {
        List<String> roleNames = List.of("USER");
        return roleNames.stream()
                .map(name -> {
                    Role role = new Role();
                    role.setName(name);
                    return roleRepository.save(role);
                })
                .collect(Collectors.toList());
    }

    private void initQuestions() {

        try (BufferedReader reader = new BufferedReader(new FileReader("backend/src/main/java/org/borg/backend/seed/questionsData"))) {
            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(",", -1);

                if (parts.length == 4) {
                    String category = parts[0].trim();
                    String question = parts[1].trim();
                    String optionsLine = parts[2].trim();
                    List<String> options = Arrays.asList(optionsLine.split(";"));
                    String correctAnswer = parts[3].trim();

                    Question questionObj = new Question();
                    questionObj.setCategory(category);
                    questionObj.setQuestion(question);
                    questionObj.setOptions(options);
                    questionObj.setCorrectAnswer(correctAnswer);
                    questionRepository.save(questionObj);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initStories() {
        Story story1 = new Story();
        story1.setTitle("The Dragon's Hoard");
        story1.setDescription("A daring adventurer journeys across dangerous lands to steal a dragon’s legendary treasure. From treacherous forests to fiery mountains, every step is fraught with peril. But the greatest test awaits at the dragon's lair.");
        story1.setIntroText("Legends speak of a dragon’s hoard, hidden deep within the heart of a distant land. Many have tried to claim it, none have returned. Now, a lone adventurer sets out to prove the myths true, braving the dangers of unknown realms for glory and wealth.");
        story1.setImagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/story_image.png");
        story1.setNumOfChapters(7);
        storyRepository.save(story1);
        
        Chapter chapter1 = Chapter.builder()
                .story(story1)
                .chapterNumber(1)
                .title("The Call to Adventure")
                .description("An ancient map and a whispered legend guide the protagonist to the start of their journey. With nothing but their wits and courage, they must leave behind their home and venture into the unknown. The first step is always the hardest.")
                .questions(getChapterQuestions(new String[]{"Science & Nature", "Sports", "Geography"}))
                .unlockCondition("NONE")
                .rewardText("You’ve answered the call. The journey is long, and the road is dangerous, but you’ve taken the first step towards the treasure. Stay vigilant.")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter1.jpeg")
                .build();
        
        
        Chapter chapter2 = Chapter.builder()
                .story(story1)
                .chapterNumber(2)
                .title("The Enchanted Forest")
                .description("The dense, mystical forest is rumored to be home to ancient creatures and hidden traps. As the protagonist delves deeper, strange noises echo through the trees. They must rely on their skills and cunning to survive the forest’s dangers.")
                .questions(getChapterQuestions(new String[]{"Film", "Books", "Geography"}))
                .unlockCondition("CHAPTER_1_COMPLETE")
                .rewardText("Surviving the forest wasn’t easy, but you’ve proved your resilience. The path forward becomes clearer with each victory.")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter2.jpeg")
                .build();
        
        Chapter chapter3 = Chapter.builder()
                .story(story1)
                .chapterNumber(3)
                .title("The Desert of Flames")
                .description("A vast, scorching desert stretches before the adventurer, where the sun beats down mercilessly. Hidden dangers lurk beneath the sand, and every step forward feels like a battle for survival. The thirst for the dragon’s treasure drives them onward.")
                .questions(getChapterQuestions(new String[]{"General Knowledge", "Music", "Sports", "Animals"}))
                .unlockCondition("CHAPTER_2_COMPLETE")
                .rewardText("The heat may have tested your endurance, but you’ve endured. Your determination is a weapon just as powerful as any sword.")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter3.jpeg")
                .build();

        Chapter chapter4 = Chapter.builder()
                .story(story1)
                .chapterNumber(4)
                .title("The Haunted Peaks")
                .description("Towering, mist-covered mountains are rumored to be haunted by spirits of the long-dead. Every step taken on these perilous cliffs feels like walking through a world of nightmares. But the adventurer’s resolve is unwavering, knowing the treasure lies ahead.")
                .questions(getChapterQuestions(new String[]{"General Knowledge", "Television", "History", "Science & Nature"}))
                .unlockCondition("CHAPTER_3_COMPLETE")
                .rewardText("You’ve braved the haunted peaks, where few dare to tread. Each challenge you face is one step closer to your ultimate goal.")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter4.jpeg")
                .build();

        Chapter chapter5 = Chapter.builder()
                .story(story1)
                .chapterNumber(5)
                .title("The Bandit’s Pass")
                .description("Narrow roads cut through jagged cliffs, where ruthless bandits lie in wait. The adventurer must outsmart these dangerous foes or fight their way through, knowing that the treasures ahead are guarded by far worse dangers.")
                .questions(getChapterQuestions(new String[]{"Japanese Anime & Manga", "General Knowledge", "Geography", "Film"}))
                .unlockCondition("CHAPTER_4_COMPLETE")
                .rewardText("Victory over the bandits proves your skill and resourcefulness. Now, you’re one step closer to the dragon’s lair.")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter5.jpeg")
                .build();

        Chapter chapter6 = Chapter.builder()
                .story(story1)
                .chapterNumber(6)
                .title("The Dragon’s Lair")
                .description("The final region looms: the lair of Ardrak, the dragon. The air is thick with the scent of smoke and fear. The hero must prepare for the ultimate confrontation—whether by stealth, trickery, or sheer force of will.")
                .questions(getChapterQuestions(new String[]{"Books", "Music", "Film", "Science & Nature", "Animals"}))
                .unlockCondition("CHAPTER_5_COMPLETE")
                .rewardText("You’ve reached the lair, but the hardest challenge lies ahead. The dragon’s treasure is within your grasp, but can you claim it?")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/story_image.png")
                .build();

        Chapter chapter7 = Chapter.builder()
                .story(story1)
                .chapterNumber(7)
                .title("The Treasure")
                .description("With the dragon defeated or outwitted, the treasure is finally within reach. But what lies within the hoard? Gold and jewels? Or something more dangerous? The adventurer must decide whether the riches are worth the cost.")
                .questions(getChapterQuestions(new String[]{"General Knowledge", "Music", "Japanese Anime & Manga", "Geography", "History"}))
                .unlockCondition("CHAPTER_6_COMPLETE")
                .rewardText("Treasure claimed, but the journey has changed you. What began as a quest for riches has become something much more. What will you do with your newfound wealth?")
                .imagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/chapter7.jpeg")
                .build();
        
        
        List<Chapter> chapters = List.of(chapter1, chapter2, chapter3, chapter4, chapter5, chapter6, chapter7);
        
        chapterRepository.saveAll(chapters);


    }

    private List<Question> getChapterQuestions(String[] categories) {
        
        List<Question> questionList = new ArrayList<>();

        for (String category : categories) {
            List<Question> subList = questionRepository.findRandomQuestionsByCategory(category);
            questionList.addAll(subList);
        }
        return questionList;
    }
}
