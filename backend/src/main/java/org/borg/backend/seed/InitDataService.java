package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.question.Question;
import org.borg.backend.question.QuestionRepository;
import org.borg.backend.story.model.Story;
import org.borg.backend.story.repository.StoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InitDataService {


    private final RoleRepository roleRepository;
    private final QuestionRepository questionRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;


    public void initRoles() {
        List<String> roleNames = List.of("USER");
        roleNames.stream()
                .map(name -> {
                    Role role = new Role();
                    role.setName(name);
                    return roleRepository.save(role);
                })
                .collect(Collectors.toList());
    }

    public void initQuestions() {

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

    @Transactional
    public void initStoryData() {
        if (storyRepository.count() > 0) {
            return;
        }

        initializeDragonsHoardStory();
    }

    private void initializeDragonsHoardStory() {
        Story dragonsHoard = createDragonsHoardStory();
        Story savedStory = storyRepository.save(dragonsHoard);

        List<Chapter> chapters = createDragonsHoardChapters(savedStory);
        chapters = chapterRepository.saveAll(chapters);

        for (Chapter chapter : chapters) {
            List<Question> questions = getChapterQuestions(chapter);
            questionRepository.saveAll(questions);
        }
    }

    private Story createDragonsHoardStory() {
        Story story = new Story();
        story.setTitle("The Dragon's Hoard");
        story.setDescription("A daring adventurer journeys across dangerous lands to steal a dragon's legendary treasure. " +
                "From treacherous forests to fiery mountains, every step is fraught with peril. " +
                "But the greatest test awaits at the dragon's lair.");
        story.setIntroText("Legends speak of a dragon's hoard, hidden deep within the heart of a distant land. " +
                "Many have tried to claim it, none have returned. Now, a lone adventurer sets out to prove " +
                "the myths true, braving the dangers of unknown realms for glory and wealth.");
        story.setImagePath("dragon_hoard/story_image.png");
        story.setNumOfChapters(7);
        return story;
    }
    
    private List<Chapter> createDragonsHoardChapters(Story story) {
        return List.of(
                createChapter(story, 1, "The Call to Adventure",
                        "An ancient map and a whispered legend guide the protagonist to the start of their journey. " +
                                "With nothing but their wits and courage, they must leave behind their home and venture into the unknown. " +
                                "The first step is always the hardest.",
                        new HashSet<>(Arrays.asList("Science & Nature", "Sports", "Geography")), "NONE",
                        "You've answered the call. The journey is long, and the road is dangerous, but you've taken the first step towards the treasure. Stay vigilant.",
                        "dragon_hoard/chapter1.jpeg"),

                createChapter(story, 2, "The Enchanted Forest",
                        "The dense, mystical forest is rumored to be home to ancient creatures and hidden traps. " +
                                "As the protagonist delves deeper, strange noises echo through the trees. " +
                                "They must rely on their skills and cunning to survive the forest's dangers.",
                        new HashSet<>(Arrays.asList("Film", "Books", "Geography")),
                        "You must complete chapter 1 in order to play this chapter",
                        "Surviving the forest wasn't easy, but you've proved your resilience. The path forward becomes clearer with each victory.",
                        "dragon_hoard/chapter2.jpeg"),

                createChapter(story, 3, "The Desert of Flames",
                        "A vast, scorching desert stretches before the adventurer, where the sun beats down mercilessly. " +
                                "Hidden dangers lurk beneath the sand, and every step forward feels like a battle for survival. " +
                                "The thirst for the dragon's treasure drives them onward.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Music", "Sports", "Animals")), "You must complete chapter 2 in order to play this chapter",
                        "The heat may have tested your endurance, but you've endured. Your determination is a weapon just as powerful as any sword.",
                        "dragon_hoard/chapter3.jpeg"),

                createChapter(story, 4, "The Haunted Peaks",
                        "Towering, mist-covered mountains are rumored to be haunted by spirits of the long-dead. " +
                                "Every step taken on these perilous cliffs feels like walking through a world of nightmares. " +
                                "But the adventurer's resolve is unwavering, knowing the treasure lies ahead.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Television", "History", "Science & Nature")), "You must complete chapter 3 in order to play this chapter",
                        "You've braved the haunted peaks, where few dare to tread. Each challenge you face is one step closer to your ultimate goal.",
                        "dragon_hoard/chapter4.jpeg"),

                createChapter(story, 5, "The Bandit's Pass",
                        "Narrow roads cut through jagged cliffs, where ruthless bandits lie in wait. " +
                                "The adventurer must outsmart these dangerous foes or fight their way through, " +
                                "knowing that the treasures ahead are guarded by far worse dangers.",
                        new HashSet<>(Arrays.asList("Japanese Anime & Manga", "General Knowledge", "Geography", "Film")), "You must complete chapter 4 in order to play this chapter",
                        "Victory over the bandits proves your skill and resourcefulness. Now, you're one step closer to the dragon's lair.",
                        "dragon_hoard/chapter5.jpeg"),

                createChapter(story, 6, "The Dragon's Lair",
                        "The final region looms: the lair of Ardrak, the dragon. The air is thick with the scent of smoke and fear. " +
                                "The hero must prepare for the ultimate confrontation—whether by stealth, trickery, or sheer force of will.",
                        new HashSet<>(Arrays.asList("Books", "Music", "Film", "Science & Nature", "Animals")), "You must complete chapter 5 in order to play this chapter",
                        "You've reached the lair, but the hardest challenge lies ahead. The dragon's treasure is within your grasp, but can you claim it?",
                        "dragon_hoard/story_image.png"),

                createChapter(story, 7, "The Treasure",
                        "With the dragon defeated or outwitted, the treasure is finally within reach. " +
                                "But what lies within the hoard? Gold and jewels? Or something more dangerous? " +
                                "The adventurer must decide whether the riches are worth the cost.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Music", "Japanese Anime & Manga", "Geography", "History")), "You must complete chapter 6 in order to play this chapter",
                        "Treasure claimed, but the journey has changed you. What began as a quest for riches has become something much more. " +
                                "What will you do with your newfound wealth?",
                        "dragon_hoard/chapter7.jpeg")
        );
    }

    private Chapter createChapter(Story story, int chapterNumber, String title, String description,
                                  HashSet<String> categories, String unlockCondition, String rewardText, String imagePath) {
        return Chapter.builder()
                .story(story)
                .chapterNumber(chapterNumber)
                .title(title)
                .description(description)
                .unlockCondition(unlockCondition)
                .categories(categories)
                .rewardText(rewardText)
                .imagePath(imagePath)
                .questions(new ArrayList<>())
                .build();
    }

    private List<Question> getChapterQuestions(Chapter chapter) {
        List<Question> questionList = new ArrayList<>();
        for (String category : chapter.getCategories()) {
            List<Question> subList = questionRepository.findRandomQuestionsByCategory(category);
            for (Question question : subList) {
                question.setChapter(chapter);
                questionList.add(question);
            }
        }
        return questionList;
    }
    
}
