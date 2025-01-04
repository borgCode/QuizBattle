package org.borg.backend.achievement.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.achievement.repository.AchievementRepository;
import org.borg.backend.achievement.repository.UserUnlockedAchievementRepository;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.model.CategoryStats;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.seed.InitDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class AchievementServiceIntegrationTest {
    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private UserUnlockedAchievementRepository userUnlockedAchievementRepository;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private InitDataService initDataService;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        achievementRepository.deleteAll();
        playerRepository.deleteAll();
        userUnlockedAchievementRepository.deleteAll();

        initDataService.initAchievements();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @Nested
    class AchievementLevelProgress {
        @Test
        void testPlayerProgressThroughAllLevels() {

        }

        @Test
        void testMultipleCategoriesProgressSimultaneously() {

        }
    }

    @Nested
    class MultiplePlayerConcurrentOperations {


        @Test
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

            List<Thread> playerThreads = players.stream()
                    .map(player -> new Thread(() -> {
                        try {
                            startLatch.await();
                            Random random = new Random();
                            
                            int numCategories = random.nextInt(3) + 2;
                            List<String> selectedCategories = new ArrayList<>(categories);
                            Collections.shuffle(selectedCategories);
                            selectedCategories = selectedCategories.subList(0, numCategories);


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
      
                Thread.sleep(1000);
 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
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

                    categoryStats.setStats(stats);
                    stats.getCategoryStats().put(category, categoryStats);
                }

                player.setStats(stats);
                playerRepository.save(player);

            }

        }

        @Test
        void multiplePlayersGetCorrectNotifications() {

        }


    }


}
