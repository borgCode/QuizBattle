package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.achievement.model.Achievement;
import org.borg.backend.achievement.model.AchievementLevel;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.question.dto.Question;
import org.borg.backend.question.repository.QuestionRepository;
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
    private final AchievementRepository achievementRepository;


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
        initializeSpaceSignalStory();
        initializeSunkenKingdomStory();
    }

    private void initializeDragonsHoardStory() {
        Story dragonsHoard = createDragonsHoardStory();
        Story savedStory = storyRepository.save(dragonsHoard);

        List<Chapter> chapters = createDragonsHoardChapters(savedStory);
        chapterRepository.saveAll(chapters);

    }

    private void initializeSunkenKingdomStory() {
        Story sunkenKingdom = createSunkenKingdomStory();
        Story savedStory = storyRepository.save(sunkenKingdom);

        List<Chapter> chapters = createSunkenKingdomChapters(savedStory);
        chapterRepository.saveAll(chapters);

    }

    private void initializeSpaceSignalStory() {
        Story spaceSignal = createSpaceSignalStory();
        Story savedStory = storyRepository.save(spaceSignal);

        List<Chapter> chapters = createSpaceSignalChapters(savedStory);
        chapterRepository.saveAll(chapters);

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
                        "dragon_hoard/chapter1.jpeg",
                        2),

                createChapter(story, 2, "The Enchanted Forest",
                        "The dense, mystical forest is rumored to be home to ancient creatures and hidden traps. " +
                                "As the protagonist delves deeper, strange noises echo through the trees. " +
                                "They must rely on their skills and cunning to survive the forest's dangers.",
                        new HashSet<>(Arrays.asList("Film", "Books", "Geography")),
                        "You must complete chapter 1 in order to play this chapter",
                        "Surviving the forest wasn't easy, but you've proved your resilience. The path forward becomes clearer with each victory.",
                        "dragon_hoard/chapter2.jpeg",
                        2),

                createChapter(story, 3, "The Desert of Flames",
                        "A vast, scorching desert stretches before the adventurer, where the sun beats down mercilessly. " +
                                "Hidden dangers lurk beneath the sand, and every step forward feels like a battle for survival. " +
                                "The thirst for the dragon's treasure drives them onward.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Music", "Sports", "Animals")), "You must complete chapter 2 in order to play this chapter",
                        "The heat may have tested your endurance, but you've endured. Your determination is a weapon just as powerful as any sword.",
                        "dragon_hoard/chapter3.jpeg",
                        2),

                createChapter(story, 4, "The Haunted Peaks",
                        "Towering, mist-covered mountains are rumored to be haunted by spirits of the long-dead. " +
                                "Every step taken on these perilous cliffs feels like walking through a world of nightmares. " +
                                "But the adventurer's resolve is unwavering, knowing the treasure lies ahead.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Television", "History", "Science & Nature")), "You must complete chapter 3 in order to play this chapter",
                        "You've braved the haunted peaks, where few dare to tread. Each challenge you face is one step closer to your ultimate goal.",
                        "dragon_hoard/chapter4.jpeg",
                        3),

                createChapter(story, 5, "The Bandit's Pass",
                        "Narrow roads cut through jagged cliffs, where ruthless bandits lie in wait. " +
                                "The adventurer must outsmart these dangerous foes or fight their way through, " +
                                "knowing that the treasures ahead are guarded by far worse dangers.",
                        new HashSet<>(Arrays.asList("Japanese Anime & Manga", "General Knowledge", "Geography", "Film")), "You must complete chapter 4 in order to play this chapter",
                        "Victory over the bandits proves your skill and resourcefulness. Now, you're one step closer to the dragon's lair.",
                        "dragon_hoard/chapter5.jpeg",
                        3),

                createChapter(story, 6, "The Dragon's Lair",
                        "The final region looms: the lair of Ardrak, the dragon. The air is thick with the scent of smoke and fear. " +
                                "The hero must prepare for the ultimate confrontation—whether by stealth, trickery, or sheer force of will.",
                        new HashSet<>(Arrays.asList("Books", "Music", "Film", "Science & Nature", "Animals")), "You must complete chapter 5 in order to play this chapter",
                        "You've reached the lair, but the hardest challenge lies ahead. The dragon's treasure is within your grasp, but can you claim it?",
                        "dragon_hoard/story_image.png",
                        3),

                createChapter(story, 7, "The Treasure",
                        "With the dragon defeated or outwitted, the treasure is finally within reach. " +
                                "But what lies within the hoard? Gold and jewels? Or something more dangerous? " +
                                "The adventurer must decide whether the riches are worth the cost.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Music", "Japanese Anime & Manga", "Geography", "History")), "You must complete chapter 6 in order to play this chapter",
                        "Treasure claimed, but the journey has changed you. What began as a quest for riches has become something much more. " +
                                "What will you do with your newfound wealth?",
                        "dragon_hoard/chapter7.jpeg",
                        3)
        );
    }

    private Story createSunkenKingdomStory() {
        Story story = new Story();
        story.setTitle("The Sunken Kingdom");
        story.setDescription("Ancient texts speak of an advanced civilization that vanished beneath the waves. " +
                "Now, cutting-edge technology has detected massive structures deep in the ocean. " +
                "A team of experts must uncover the truth behind this legendary lost kingdom.");
        story.setIntroText("When satellite scans reveal geometric patterns on the ocean floor, it confirms " +
                "centuries of myths about a forgotten civilization. As part of an elite research team, " +
                "you must dive into the depths to uncover humanity's greatest archaeological mystery.");
        story.setImagePath("sunken_kingdom/story_image.png");
        story.setNumOfChapters(7);
        return story;
    }

    private List<Chapter> createSunkenKingdomChapters(Story story) {
        return List.of(
                createChapter(story, 1, "The Ancient Scroll",
                        "A newly discovered manuscript provides precise coordinates for the lost kingdom. " +
                                "As researchers decipher its cryptic symbols, they realize this civilization " +
                                "was far more advanced than anyone imagined.",
                        new HashSet<>(Arrays.asList("History", "Geography", "Books")), "NONE",
                        "The scroll's secrets are revealed. Each translation brings us closer to finding " +
                                "this legendary civilization.",
                        "sunken_kingdom/chapter1.jpg",
                        2),

                createChapter(story, 2, "Ocean's Gateway",
                        "The expedition reaches the coordinates. Advanced sonar reveals a massive underwater " +
                                "plateau with clear signs of artificial construction. The team must prepare " +
                                "for the first descent into these mysterious waters.",
                        new HashSet<>(Arrays.asList("Science & Nature", "Film", "Animals")),
                        "You must complete chapter 1 in order to play this chapter",
                        "The structures below are unlike anything in recorded history. What secrets await in the depths?",
                        "sunken_kingdom/chapter2.png",
                        2),

                createChapter(story, 3, "The First Descent",
                        "Using state-of-the-art diving equipment, the team makes their first journey to " +
                                "the sunken ruins. They discover perfectly preserved buildings and strange " +
                                "symbols that pulse with an unexplained energy.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Television", "Sports", "Music")),
                        "You must complete chapter 2 in order to play this chapter",
                        "The descent revealed wonders beyond imagination. This is just the beginning of our discoveries.",
                        "sunken_kingdom/chapter3.jpg",
                        2),

                createChapter(story, 4, "City of Wonders",
                        "The team discovers an entire city preserved in an underwater dome. Advanced technology " +
                                "seems to keep the water at bay, but how? And more importantly, what happened " +
                                "to the people who built all this?",
                        new HashSet<>(Arrays.asList("Japanese Anime & Manga", "Science & Nature", "History", "Film")),
                        "You must complete chapter 3 in order to play this chapter",
                        "The city's technological marvels challenge everything we thought we knew about ancient civilizations.",
                        "sunken_kingdom/story_image.png",
                        3),

                createChapter(story, 5, "The Great Archive",
                        "Deep within the city lies a vast library of crystalline devices. Each crystal seems " +
                                "to contain stored knowledge, but accessing it requires solving complex puzzles " +
                                "left by the ancient inhabitants.",
                        new HashSet<>(Arrays.asList("Books", "Geography", "General Knowledge", "Music")),
                        "You must complete chapter 4 in order to play this chapter",
                        "The crystals hold the history of a civilization lost to time. Their story must be told.",
                        "sunken_kingdom/chapter5.jpg",
                        3),

                createChapter(story, 6, "The Warning",
                        "As the team decodes more crystals, they uncover a troubling truth. The civilization " +
                                "didn't just vanish - they were running from something. Warning messages speak " +
                                "of a catastrophic event that forced them to abandon their city.",
                        new HashSet<>(Arrays.asList("Television", "Science & Nature", "Animals", "Books")),
                        "You must complete chapter 5 in order to play this chapter",
                        "The truth behind their disappearance is more disturbing than we imagined. Are we ready " +
                                "to face the same challenge?",
                        "sunken_kingdom/chapter6.jpg",
                        3),

                createChapter(story, 7, "Legacy of the Deep",
                        "With time running out and pressure mounting from above, the team must decide what " +
                                "to do with their discoveries. The ancient civilization left behind both " +
                                "remarkable knowledge and a dire warning about humanity's future.",
                        new HashSet<>(Arrays.asList("History", "Japanese Anime & Manga", "Geography", "General Knowledge")),
                        "You must complete chapter 6 in order to play this chapter",
                        "The lost kingdom's legacy now rests in our hands. Their past may well be crucial to our future.",
                        "sunken_kingdom/chapter7.jpg",
                        3)
        );
    }

    private Story createSpaceSignalStory() {
        Story story = new Story();
        story.setTitle("The Cosmic Signal");
        story.setDescription("A mysterious signal from deep space leads humanity's best team of explorers on an " +
                "interstellar journey to uncover its source. As they venture further into unknown space, " +
                "they discover that the signal might hold the key to humanity's biggest questions.");
        story.setIntroText("When Earth's most powerful telescope detects an unusual pattern of signals from " +
                "a distant star system, it sparks humanity's greatest space expedition. As part of an elite " +
                "crew, you must navigate through the unknown reaches of space to find the signal's source.");
        story.setImagePath("cosmic_signal/story_image.jpg");
        story.setNumOfChapters(7);
        return story;
    }

    private List<Chapter> createSpaceSignalChapters(Story story) {
        return List.of(
                createChapter(story, 1, "Launch Sequence",
                        "The most advanced spacecraft ever built stands ready for launch. As final preparations " +
                                "are made, the crew must ensure all systems are functioning perfectly. Every detail " +
                                "matters when preparing for humanity's most ambitious journey.",
                        new HashSet<>(Arrays.asList("Science & Nature", "Geography", "General Knowledge")), "NONE",
                        "The launch was successful. As Earth shrinks behind us, the mysteries of space beckon ahead. " +
                                "The real journey is just beginning.",
                        "cosmic_signal/chapter1.jpg",
                        2),

                createChapter(story, 2, "Solar System Farewell",
                        "Navigating through our solar system presents its own challenges. The ship must thread " +
                                "its way through asteroid fields and use planetary gravity wells to gain speed. " +
                                "Each maneuver brings the crew closer to the edge of known space.",
                        new HashSet<>(Arrays.asList("Science & Nature", "History", "Film")),
                        "You must complete chapter 1 in order to play this chapter",
                        "The familiar planets are now behind us. Ahead lies the vast unknown of interstellar space.",
                        "cosmic_signal/chapter2.jpg",
                        2),

                createChapter(story, 3, "The Void Between",
                        "In the darkness between stars, the crew faces psychological and technical challenges. " +
                                "The signal grows stronger, but strange equipment malfunctions begin to occur. " +
                                "The ship's AI detects anomalies that defy explanation.",
                        new HashSet<>(Arrays.asList("Books", "Television", "Music", "General Knowledge")),
                        "You must complete chapter 2 in order to play this chapter",
                        "The mysteries deepen, but your crew's resolve remains strong. The signal's source grows closer.",
                        "cosmic_signal/chapter3.jpg",
                        2),

                createChapter(story, 4, "First Contact Point",
                        "The signal leads to an uncharted star system. Unusual energy readings suggest advanced " +
                                "technology, but no signs of current civilization. The crew must investigate a series " +
                                "of artificial structures orbiting a dying star.",
                        new HashSet<>(Arrays.asList("Science & Nature", "Film", "Japanese Anime & Manga", "Sports")),
                        "You must complete chapter 3 in order to play this chapter",
                        "These structures hold secrets beyond our understanding. Each discovery leads to more questions.",
                        "cosmic_signal/chapter4.jpg",
                        3),

                createChapter(story, 5, "The Ancient Station",
                        "The signal emanates from a massive space station, clearly built by an advanced civilization. " +
                                "But where are they now? The crew must dock with the station and prepare to board, " +
                                "facing unknown risks in an alien environment.",
                        new HashSet<>(Arrays.asList("History", "Books", "Animals", "Geography")),
                        "You must complete chapter 4 in order to play this chapter",
                        "The station has been waiting for millennia. Now, its secrets are within reach.",
                        "cosmic_signal/chapter5.jpg",
                        3),

                createChapter(story, 6, "Decoding the Message",
                        "Inside the station, the crew discovers a vast archive of alien knowledge. The signal " +
                                "appears to be part of an automated system, but its purpose remains unclear. Time " +
                                "is running out as the station's power systems show signs of critical failure.",
                        new HashSet<>(Arrays.asList("General Knowledge", "Music", "Television", "Science & Nature")),
                        "You must complete chapter 5 in order to play this chapter",
                        "The message is more profound than anyone imagined. Humanity's understanding of the universe " +
                                "will never be the same.",
                        "cosmic_signal/chapter6.jpg",
                        3),

                createChapter(story, 7, "The Return Journey",
                        "With the station's power failing, the crew must race to preserve its knowledge and escape. " +
                                "But the greatest challenge lies in comprehending the implications of their discovery. " +
                                "What message will they bring back to Earth?",
                        new HashSet<>(Arrays.asList("Film", "Geography", "Japanese Anime & Manga", "History")),
                        "You must complete chapter 6 in order to play this chapter",
                        "You carry humanity's greatest discovery home. The signal was just the beginning - " +
                                "our journey to the stars has truly begun.",
                        "cosmic_signal/chapter7.jpg",
                        3)
        );
    }

    private Chapter createChapter(Story story, int chapterNumber, String title, String description,
                                  HashSet<String> categories, String unlockCondition, String rewardText, String imagePath,
                                  int roundWinCondition) {
        return Chapter.builder()
                .story(story)
                .chapterNumber(chapterNumber)
                .title(title)
                .description(description)
                .unlockCondition(unlockCondition)
                .categories(categories)
                .rewardText(rewardText)
                .imagePath(imagePath)
                .roundWinCondition(roundWinCondition)
                .build();
    }


    public void initAchievements() {
//        initCategoryAchievements();
//        initStoryAchievements();
    }

    private void initCategoryAchievements() {
        createAchievement(
                "Geography",
                List.of(
                        AchievementLevel.builder()
                                .name("Globe Trotter")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 geography questions correctly")
                                .imageUrl("/images/achievements/geography-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("World Explorer")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 geography questions correctly")
                                .imageUrl("/images/achievements/geography-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Geography Sage")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 geography questions correctly")
                                .imageUrl("/images/achievements/geography-3.png")
                                .build()
                )
        );

        createAchievement(
                "General Knowledge",
                List.of(
                        AchievementLevel.builder()
                                .name("Curious Mind")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 general knowledge questions correctly")
                                .imageUrl("/images/achievements/knowledge-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Scholar")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 general knowledge questions correctly")
                                .imageUrl("/images/achievements/knowledge-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Polymath")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 general knowledge questions correctly")
                                .imageUrl("/images/achievements/knowledge-3.png")
                                .build()
                )
        );

        createAchievement(
                "Science & Nature",
                List.of(
                        AchievementLevel.builder()
                                .name("Lab Assistant")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 science questions correctly")
                                .imageUrl("/images/achievements/science-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Researcher")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 science questions correctly")
                                .imageUrl("/images/achievements/science-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Chief Scientist")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 science questions correctly")
                                .imageUrl("/images/achievements/science-3.png")
                                .build()
                )
        );


        createAchievement(
                "Film",
                List.of(
                        AchievementLevel.builder()
                                .name("Movie Fan")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 film questions correctly")
                                .imageUrl("/images/achievements/film-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Film Critic")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 film questions correctly")
                                .imageUrl("/images/achievements/film-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Cinema Legend")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 film questions correctly")
                                .imageUrl("/images/achievements/film-3.png")
                                .build()
                )
        );

        createAchievement(
                "History",
                List.of(
                        AchievementLevel.builder()
                                .name("Time Traveler")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 history questions correctly")
                                .imageUrl("/images/achievements/history-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Historian")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 history questions correctly")
                                .imageUrl("/images/achievements/history-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("History Maven")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 history questions correctly")
                                .imageUrl("/images/achievements/history-3.png")
                                .build()
                )
        );

        createAchievement(
                "Television",
                List.of(
                        AchievementLevel.builder()
                                .name("Channel Surfer")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 television questions correctly")
                                .imageUrl("/images/achievements/tv-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Series Expert")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 television questions correctly")
                                .imageUrl("/images/achievements/tv-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("TV Virtuoso")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 television questions correctly")
                                .imageUrl("/images/achievements/tv-3.png")
                                .build()
                )
        );

        createAchievement(
                "Music",
                List.of(
                        AchievementLevel.builder()
                                .name("Melody Maker")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 music questions correctly")
                                .imageUrl("/images/achievements/music-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Harmony Master")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 music questions correctly")
                                .imageUrl("/images/achievements/music-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Symphony Sage")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 music questions correctly")
                                .imageUrl("/images/achievements/music-3.png")
                                .build()
                )
        );


        createAchievement(
                "Books",
                List.of(
                        AchievementLevel.builder()
                                .name("Bookworm")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 book questions correctly")
                                .imageUrl("/images/achievements/books-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Literature Lover")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 book questions correctly")
                                .imageUrl("/images/achievements/books-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Master Bibliophile")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 book questions correctly")
                                .imageUrl("/images/achievements/books-3.png")
                                .build()
                )
        );


        createAchievement(
                "Japanese Anime & Manga",
                List.of(
                        AchievementLevel.builder()
                                .name("Otaku Initiate")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 anime questions correctly")
                                .imageUrl("/images/achievements/anime-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Manga Master")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 anime questions correctly")
                                .imageUrl("/images/achievements/anime-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Anime Sage")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 anime questions correctly")
                                .imageUrl("/images/achievements/anime-3.png")
                                .build()
                )
        );


        createAchievement(
                "Sports",
                List.of(
                        AchievementLevel.builder()
                                .name("Team Player")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 sports questions correctly")
                                .imageUrl("/images/achievements/sports-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("MVP")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 sports questions correctly")
                                .imageUrl("/images/achievements/sports-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Sports Legend")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 sports questions correctly")
                                .imageUrl("/images/achievements/sports-3.png")
                                .build()
                )
        );

        createAchievement(
                "Animals",
                List.of(
                        AchievementLevel.builder()
                                .name("Animal Friend")
                                .level(1)
                                .requirementValue(5)
                                .description("Answer 5 animal questions correctly")
                                .imageUrl("/images/achievements/animals-1.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Wildlife Specialist")
                                .level(2)
                                .requirementValue(10)
                                .description("Answer 10 animal questions correctly")
                                .imageUrl("/images/achievements/animals-2.png")
                                .build(),
                        AchievementLevel.builder()
                                .name("Zoology Master")
                                .level(3)
                                .requirementValue(15)
                                .description("Answer 15 animal questions correctly")
                                .imageUrl("/images/achievements/animals-3.png")
                                .build()
                )
        );
    }

    private void initStoryAchievements() {
        createAchievement(
                "The Dragon's Hoard",
                List.of(
                        AchievementLevel.builder()
                                .name("")
                                .level(1)
                                .requirementValue(1)
                                .description("Complete The Dragon's Hoard story")
                                .imageUrl("/images/achievements/dragon")
                                .build()
                )
        );
        createAchievement(
                "The Cosmic Signal",
                List.of(
                        AchievementLevel.builder()
                                .name("")
                                .level(1)
                                .requirementValue(1)
                                .description("Complete The Cosmic Signal story")
                                .imageUrl("/images/achievements/cosmic")
                                .build()
                )
        );
        createAchievement(
                "The Sunken Kingdom",
                List.of(
                        AchievementLevel.builder()
                                .name("")
                                .level(1)
                                .requirementValue(1)
                                .description("Complete The Sunken Kingdom story")
                                .imageUrl("/images/achievements/sunken")
                                .build()
                )
        );
    }

    private void createAchievement(String name, List<AchievementLevel> levels) {
        Achievement achievement = new Achievement();
        achievement.setName(name);

        levels.forEach(level -> level.setAchievement(achievement));
        achievement.setLevels(levels);

        achievementRepository.save(achievement);
    }
}
