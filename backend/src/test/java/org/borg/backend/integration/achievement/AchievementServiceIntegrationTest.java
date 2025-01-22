package org.borg.backend.integration.achievement;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.model.*;
import org.borg.backend.player.repository.*;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.seed.InitDataService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class AchievementServiceIntegrationTest {
    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private InitDataService initDataService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AchievementProgressRepository achievementProgressRepository;
    @Autowired
    private AchievementLevelHistoryRepository achievementLevelHistoryRepository;

    @BeforeEach
    void setUp() {
        achievementRepository.deleteAll();
        playerRepository.deleteAll();
       

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @AfterEach
    void tearDown() {
        playerRepository.deleteAll();
        achievementRepository.deleteAll();
    }

    @Nested
    class AchievementLevelProgress {
        private Player player;

        @BeforeEach
        void setUp() {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

            player = Player.builder()
                    .username("testPlayer")
                    .password("password")
                    .displayName("Test Player")
                    .stats(new Stats())
                    .accountLocked(false)
                    .enabled(true)
                    .roles(new ArrayList<>(List.of(userRole)))
                    .build();
        }

        @Test
        void testCategoryAchievementProgressThroughAllLevels() {
            createAchievements();

            CategoryStats categoryStats = CategoryStats.builder()
                    .category("Geography")
                    .correct(0)
                    .questionsAnswered(0)
                    .build();

            Map<String, CategoryStats> statsMap = new HashMap<>();
            statsMap.put("Geography", categoryStats);
            player.getStats().setCategoryStats(statsMap);
            
            player = playerRepository.save(player);

            increaseStatsToNextLevelAndPublish(5);
            assertUnlockedAchievement(1);
            increaseStatsToNextLevelAndPublish(5);
            assertUnlockedAchievement(2);
            increaseStatsToNextLevelAndPublish(5);
            assertUnlockedAchievement(3);
        }
        

        private void createAchievements() {
            List<AchievementLevel> levels = List.of(
                    AchievementLevel.builder()
                            .name("Globe Trotter")
                            .level(1)
                            .requirementValue(5)
                            .description("Answer 5 geography questions correctly")
                            .imageUrl("geography-bronze.png")
                            .build(),
                    AchievementLevel.builder()
                            .name("World Explorer")
                            .level(2)
                            .requirementValue(10)
                            .description("Answer 10 geography questions correctly")
                            .imageUrl("geography-silver.png")
                            .build(),
                    AchievementLevel.builder()
                            .name("Geography Sage")
                            .level(3)
                            .requirementValue(15)
                            .description("Answer 15 geography questions correctly")
                            .imageUrl("geography-gold.png")
                            .build()
            );

            Achievement achievement = new Achievement();
            achievement.setName("Geography");

            levels.forEach(level -> level.setAchievement(achievement));
            achievement.setLevels(levels);

            achievementRepository.save(achievement);
        }

        private void increaseStatsToNextLevelAndPublish(int i) {
            log.warn("Increase to next level");
            CategoryStats categoryStats = player.getStats().getCategoryStats().get("Geography");
            int previousCorrect = categoryStats.getCorrect();

            categoryStats.setCorrect(previousCorrect + i);
            playerRepository.save(player);

            assertEquals(previousCorrect + i, categoryStats.getCorrect());

            applicationEventPublisher.publishEvent(new AchievementEvents.CategoryCompletedEvent(player.getId(), "Geography"));
        }

        private void assertUnlockedAchievement(int achievementLevel) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Achievement achievement = achievementRepository.findByName("Geography");
            AchievementProgress progress = achievementProgressRepository.findByPlayerAndAchievement(player, achievement);
            List<AchievementLevelHistory> history = achievementLevelHistoryRepository.findByPlayerAndAchievement(player, achievement);

            int maxLevel = 3;
            int expectedProgressLevel = achievementLevel == maxLevel ? maxLevel : achievementLevel + 1;

            assertAll("Check achievement progress details",
                    () -> assertNotNull(progress, "Achievement progress should exist"),
                    () -> assertEquals(expectedProgressLevel, progress.getCurrentLevel().getLevel(),
                            String.format("Progress level should be %s", achievementLevel == maxLevel ? "at max level" : "one ahead of achieved level")),
                    () -> assertTrue(history.stream().anyMatch(h -> h.getAchievementLevel().getLevel() == achievementLevel),
                            "History should contain the achieved level")
            );
        }
    }

    @Nested
    class MultiplePlayerConcurrentOperations {

        @BeforeEach
        void setUp() {
            initDataService.initAchievements();
        }

        @RepeatedTest(5)
        void multiplePlayersUnlockAchievementsSimultaneously() {
            List<String> categories = Arrays.asList(
                    "Science & Nature",
                    "History",
                    "Geography",
                    "Sports",
                    "Film",
                    "Books",
                    "Animals"
            );

            Map<Long, Set<String>> playerAwardedAchievements = new ConcurrentHashMap<>();

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            List<Player> players = transactionTemplate.execute(status -> {
                List<Player> createdPlayers = createTestPlayers();
                return playerRepository.saveAll(createdPlayers);
            });

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(players.size());

            transactionTemplate.execute(status -> {
                updateStats(players, categories);
                return null;
            });

            Random random = new Random();
            List<Thread> playerThreads = players.stream()
                    .map(player -> new Thread(() -> {
                        try {
                            startLatch.await();

                            int numCategories = random.nextInt(3) + 2;
                            List<String> selectedCategories = new ArrayList<>(categories);
                            Collections.shuffle(selectedCategories);
                            selectedCategories = selectedCategories.subList(0, numCategories);

                            playerAwardedAchievements.put(player.getId(), ConcurrentHashMap.newKeySet());

                            for (String category : selectedCategories) {
                                CategoryStats stats = player.getStats().getCategoryStats().get(category);
                                stats.setCorrect(5);
                                stats.setQuestionsAnswered(5);

                                playerRepository.save(player);

                                applicationEventPublisher.publishEvent(
                                        new AchievementEvents.CategoryCompletedEvent(
                                                player.getId(),
                                                category
                                        )
                                );
                                playerAwardedAchievements.get(player.getId()).add(category);
                            }

                            finishLatch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }))
                    .toList();

            playerThreads.forEach(Thread::start);
            startLatch.countDown();

            try {
                boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
                assertTrue(completed, "Not all achievement operations completed in time");

                Thread.sleep(100);

                for (Player player : players) {
                    Set<String> playedCategories = playerAwardedAchievements.get(player.getId());
                    assertNotNull(playedCategories, "No achievements recorded for this player");

                    for (String playedCategory : playedCategories) {
                        Achievement achievement = achievementRepository.findByName(playedCategory);
                        AchievementProgress progress = achievementProgressRepository.findByPlayerAndAchievement(player, achievement);
                        List<AchievementLevelHistory> history = achievementLevelHistoryRepository.findByPlayerAndAchievement(player, achievement);

                        assertAll(String.format("Check achievements for player %d, category %s", player.getId(), playedCategory),
                                () -> assertNotNull(progress, "Achievement progress should exist"),
                                () -> assertNotNull(progress.getCurrentLevel(), "Current level should be set"),
                                () -> assertEquals(2, progress.getCurrentLevel().getLevel(), "Progress should be at level 2"),
                                () -> assertFalse(history.isEmpty(), "Should have history entries"),
                                () -> assertTrue(history.stream().anyMatch(h -> h.getAchievementLevel().getLevel() == 1),
                                        "Should have history entry for level 1")
                        );
                    }

                    List<AchievementProgress> progressList = achievementProgressRepository.findAllByPlayer(player);
                    assertEquals(playedCategories.size(), progressList.size(),
                            String.format("Number of achievements mismatched for player: %s", player.getId()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private List<Player> createTestPlayers() {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));
            List<Player> players = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {
                Player player = Player.builder()
                        .username("testPlayer" + i)
                        .password("password")
                        .displayName("Test Player " + i)
                        .stats(new Stats())
                        .accountLocked(false)
                        .enabled(true)
                        .roles(new ArrayList<>(List.of(userRole)))
                        .build();

                players.add(player);
            }
            return players;
        }

        private void updateStats(List<Player> players, List<String> categories) {
            for (Player player : players) {
                Stats stats = player.getStats();
                for (String category : categories) {
                    CategoryStats categoryStats = CategoryStats.builder()
                            .category(category)
                            .correct(0)
                            .questionsAnswered(0)
                            .build();

                    stats.getCategoryStats().put(category, categoryStats);
                }
                playerRepository.save(player);
            }
        }
    }
}
