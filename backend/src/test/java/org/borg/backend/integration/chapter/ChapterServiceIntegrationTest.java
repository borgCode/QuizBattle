package org.borg.backend.integration.chapter;

import Config.TestDataLoader;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.game.singleplayer.service.ChapterService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.StoryProgress;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.repository.StoryProgressRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.AchievementService;
import org.borg.backend.seed.InitDataService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestDataLoader.class)
class ChapterServiceIntegrationTest {

    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private StoryProgressRepository storyProgressRepository;
    @Autowired
    private ChapterProgressRepository chapterProgressRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private InitDataService initDataService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private ChapterService chapterService;
    @Autowired
    private TestDataLoader testDataLoader;
    
    @MockitoBean
    private AchievementService achievementService;

    @BeforeAll
    void setUpOnce() {
        chapterProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        storyProgressRepository.deleteAll();
        playerRepository.deleteAll();
        storyRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
        initDataService.initStoryData();
    }

    @AfterAll
    void cleanUpAll() {
        chapterProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        storyProgressRepository.deleteAll();
        playerRepository.deleteAll();
        storyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        chapterProgressRepository.deleteAll();
        storyProgressRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Nested
    class chapterUpdateTests {
        StoryProgress storyProgress;
        List<Chapter> chapters;
        Player player;
        Story story;

        @BeforeEach
        void setUp() {
            player = testDataLoader.createTestPlayer();

            playerRepository.save(player);

            story = storyRepository.findById(1L)
                    .orElseThrow();

            storyProgress = createStoryProgress(player, story);
            
            chapters = chapterRepository.findByStoryId(story.getId());
        }
        

        @Test
        void shouldNotIncrementCompleteChaptersOnDuplicateCompletion() {
            chapterService.startChapter(new StartChapterRequest(
                    player.getId(),
                    story.getId(),
                    chapters.get(0).getId()
            ));

            ChapterProgress firstProgress = chapterProgressRepository
                    .findByStoryProgressIdAndChapterId(storyProgress.getId(), chapters.get(0).getId());
            chapterService.updateChapterProgress(firstProgress.getId());

            chapterService.startChapter(new StartChapterRequest(
                    player.getId(),
                    story.getId(),
                    chapters.get(0).getId()
            ));

            ChapterProgress secondProgress = chapterProgressRepository
                    .findByStoryProgressIdAndChapterId(storyProgress.getId(), chapters.get(0).getId());
            chapterService.updateChapterProgress(secondProgress.getId());

            StoryProgress progressAfterDuplicate = storyProgressRepository
                    .findByPlayerIdAndStoryId(player.getId(), story.getId());
            assertEquals(1, progressAfterDuplicate.getCompletedChapters(),
                    "Completing same chapter twice should not increment counter");
        }
    }

    private StoryProgress createStoryProgress(Player player, Story story) {
        return storyProgressRepository.save(StoryProgress.builder()
                .player(player)
                .story(story)
                .completedChapters(0)
                .progressStatus(ProgressStatus.NOT_STARTED).build());
    }

    @RepeatedTest(5)
    void multiplePlayersInitiateChaptersSimultaneously() {

        Map<Long, Long> playerStoryIds = new ConcurrentHashMap<>();
        Map<Long, StoryProgress> storyProgressMap = new ConcurrentHashMap<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        List<Player> players = transactionTemplate.execute(status -> {
            List<Player> createdPlayers = createTestPlayers();
            return playerRepository.saveAll(createdPlayers);
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(players.size());

        Random random = new Random();

        List<Thread> playerThreads = players.stream()
                .map(player -> new Thread(() -> {
                    try {
                        startLatch.await();

                        Long storyId = random.nextLong(3) + 1;

                        playerStoryIds.put(player.getId(), storyId);

                        Story story = storyRepository.findById(storyId)
                                .orElseThrow();
                        StoryProgress storyProgress = createStoryProgress(player, story);

                        List<Chapter> chapters = chapterRepository.findByStoryId(storyId);

                        chapterService.startChapter(new StartChapterRequest(
                                player.getId(),
                                storyId,
                                chapters.get(0).getId()
                        ));

                        storyProgressMap.put(player.getId(), storyProgress);

                        finishLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })).toList();

        playerThreads.forEach(Thread::start);
        startLatch.countDown();

        try {
            boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "Timed out waiting for all threads to complete");

            assertEquals(players.size(), storyProgressMap.size(),
                    "All players should have progress entries");

            storyProgressMap.forEach((playerId, progress) -> {
                Optional<StoryProgress> storedProgress = storyProgressRepository
                        .findById(progress.getId());
                assertTrue(storedProgress.isPresent());

                assertEquals(playerId, storedProgress.get().getPlayer().getId(),
                        "Player Id doesn't match player ID linked to StoryProgress");

                Long playerSelectedStoryId = playerStoryIds.get(playerId);
                assertEquals(playerSelectedStoryId, storedProgress.get().getStory().getId(),
                        "Story ID doesn't match selected story");
            });
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
                    .accountLocked(false)
                    .enabled(true)
                    .roles(new ArrayList<>(List.of(userRole)))
                    .build();

            players.add(player);
        }
        return players;
    }
}