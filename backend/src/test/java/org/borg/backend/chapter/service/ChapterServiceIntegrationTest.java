package org.borg.backend.chapter.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.service.AchievementService;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.chapter.dto.InitiateProgressRequest;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.chapter.model.ChapterProgress;
import org.borg.backend.chapter.repository.ChapterProgressRepository;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerProgress;
import org.borg.backend.player.repository.PlayerProgressRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.seed.InitDataService;
import org.borg.backend.shared.enums.ProgressStatus;
import org.borg.backend.story.model.Story;
import org.borg.backend.story.repository.StoryRepository;
import org.borg.backend.story.service.StoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChapterServiceIntegrationTest {

    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private PlayerProgressRepository playerProgressRepository;
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
    private StoryService storyService;
    @Autowired
    private ChapterService chapterService;
    @MockitoBean
    private AchievementService achievementService;

    @BeforeAll
    void setUpOnce() {
        chapterProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        playerProgressRepository.deleteAll();
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
        playerProgressRepository.deleteAll();
        playerRepository.deleteAll();
        storyRepository.deleteAll();
    }


    @AfterEach
    void tearDown() {
        chapterProgressRepository.deleteAll();
        playerProgressRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Nested
    class chapterUpdateTests {
        PlayerProgress playerProgress;
        List<Chapter> chapters;
        Player player;
        Story story;

        @BeforeEach
        void setUp() {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

            player = Player.builder()
                    .username("testPlayer")
                    .password("password")
                    .displayName("Test Player ")
                    .accountLocked(false)
                    .enabled(true)
                    .roles(new ArrayList<>(List.of(userRole)))
                    .build();

            playerRepository.save(player);

            story = storyRepository.findById(1L)
                    .orElseThrow();

            playerProgress = storyService.getOrCreatePlayerProgress(player.getId(), story);
            chapters = chapterRepository.findByStoryId(story.getId());
        }
        
        @Test
        void shouldUpdateChapterProgressAndCompleteStory() {
//            int expectedCompleteChapters = 1;
//            for (int i = 0; i < chapters.size() - 1; i++) {
//                initChapterProgressAndAssertStatus(chapters.get(i).getId(), expectedCompleteChapters);
//                expectedCompleteChapters++;
//            }
//            
//            chapterService.startChapter(new InitiateProgressRequest(
//                    player.getId(),
//                    playerProgress.getId(),
//                    story.getId(),
//                    chapters.get(chapters.size() - 1).getId()
//            ));
//
//            ChapterProgress lastChapterProgress = chapterProgressRepository
//                    .findByPlayerProgressIdAndChapterId(playerProgress.getId(),
//                            chapters.get(chapters.size() - 1).getId());
//
//            chapterService.updateChapterProgress(lastChapterProgress.getId());
//
//            PlayerProgress newPlayerProgress = playerProgressRepository
//                    .findByPlayerIdAndStoryId(player.getId(), story.getId());
//
//            assertAll("Post story-complete progress checks",
//                    () -> assertEquals(ProgressStatus.COMPLETED,
//                            lastChapterProgress.getProgressStatus(),
//                            "Chapter should be marked as complete"),
//                    () -> assertEquals(story.getNumOfChapters(),
//                            newPlayerProgress.getCompletedChapters(),
//                            "Player progress completed chapters should be incremented"),
//                    () -> assertEquals(ProgressStatus.COMPLETED,
//                            newPlayerProgress.getProgressStatus(),
//                            "Story should be marked as complete")
//            );

        }

        private void initChapterProgressAndAssertStatus(Long chapterId, int expectedCompleteChapters) {
            chapterService.startChapter(new InitiateProgressRequest(
                    player.getId(),
                    story.getId(),
                    chapterId
            ));
            
            ChapterProgress chapterProgress = chapterProgressRepository
                    .findByPlayerProgressIdAndChapterId(playerProgress.getId(), chapterId);

            chapterService.updateChapterProgress(chapterProgress.getId());

            assertAll("Post chapter-update progress checks",
                    () -> assertEquals(ProgressStatus.COMPLETED,
                            chapterProgressRepository.findById(chapterProgress.getId())
                                    .get()
                                    .getProgressStatus(),
                            "Chapter should be marked as complete"),
                    () -> assertEquals(expectedCompleteChapters,
                            playerProgressRepository.findByPlayerIdAndStoryId(player.getId(), story.getId())
                                    .getCompletedChapters(),
                            "Player progress completed chapters should be incremented")
            );

        }
        @Test
        void shouldNotIncrementCompleteChaptersOnDuplicateCompletion() {
            chapterService.startChapter(new InitiateProgressRequest(
                    player.getId(),
                    story.getId(),
                    chapters.get(0).getId()
            ));

            ChapterProgress firstProgress = chapterProgressRepository
                    .findByPlayerProgressIdAndChapterId(playerProgress.getId(), chapters.get(0).getId());
            chapterService.updateChapterProgress(firstProgress.getId());
            
            chapterService.startChapter(new InitiateProgressRequest(
                    player.getId(),
                    story.getId(),
                    chapters.get(0).getId()
            ));

            ChapterProgress secondProgress = chapterProgressRepository
                    .findByPlayerProgressIdAndChapterId(playerProgress.getId(), chapters.get(0).getId());
            chapterService.updateChapterProgress(secondProgress.getId());

            PlayerProgress progressAfterDuplicate = playerProgressRepository
                    .findByPlayerIdAndStoryId(player.getId(), story.getId());
            assertEquals(1, progressAfterDuplicate.getCompletedChapters(),
                    "Completing same chapter twice should not increment counter");
        }
        


    }

    @RepeatedTest(5)
    void multiplePlayersInitiateChaptersSimultaneously() {

        Map<Long, Long> playerStoryIds = new ConcurrentHashMap<>();
        Map<Long, PlayerProgress> playerProgresses = new ConcurrentHashMap<>();

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
                        log.warn("StoryId: " + storyId);

                        playerStoryIds.put(player.getId(), storyId);

                        Story story = storyRepository.findById(storyId)
                                .orElseThrow();
                        PlayerProgress playerProgress = storyService
                                .getOrCreatePlayerProgress(player.getId(), story);

                        List<Chapter> chapters = chapterRepository.findByStoryId(storyId);

                        chapterService.startChapter(new InitiateProgressRequest(
                                player.getId(),
                                storyId,
                                chapters.get(0).getId()
                        ));
                        
                        playerProgresses.put(player.getId(), playerProgress);

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

            assertEquals(players.size(), playerProgresses.size(),
                    "All players should have progress entries");

            playerProgresses.forEach((playerId, progress) -> {
                Optional<PlayerProgress> storedProgress = playerProgressRepository
                        .findById(progress.getId());
                assertTrue(storedProgress.isPresent());

                assertEquals(playerId, storedProgress.get().getPlayer().getId(),
                        "Player Id doesn't match player ID linked to PlayerProgress");

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