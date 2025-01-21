package org.borg.backend.unit.game.singleplayer;

import org.borg.backend.game.singleplayer.dto.StartChapterRequest;
import org.borg.backend.game.singleplayer.model.Chapter;
import org.borg.backend.game.singleplayer.model.ChapterProgress;
import org.borg.backend.game.singleplayer.model.Story;
import org.borg.backend.game.singleplayer.repository.ChapterProgressRepository;
import org.borg.backend.game.singleplayer.repository.ChapterRepository;
import org.borg.backend.game.singleplayer.repository.StoryRepository;
import org.borg.backend.game.singleplayer.service.ChapterService;
import org.borg.backend.game.singleplayer.service.ChapterSessionService;
import org.borg.backend.game.singleplayer.service.StoryService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.ProgressStatus;
import org.borg.backend.player.model.StoryProgress;
import org.borg.backend.player.repository.StoryProgressRepository;
import org.borg.backend.player.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ChapterServiceTest {
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private StoryProgressRepository storyProgressRepository;
    @Mock
    private ChapterProgressRepository chapterProgressRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ChapterSessionService chapterSessionService;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private StoryService storyService;

    @InjectMocks
    private ChapterService chapterService;

    private Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player = Player.builder()
                .id(1L)
                .username("player1")
                .password("oldPass")
                .build();
    }

    @Nested
    class StartChapterTests {
        StartChapterRequest request;
        Chapter chapter;

        @BeforeEach
        void setUp() {
            request = new StartChapterRequest(player.getId(), 1L, 1L);
            chapter = new Chapter();
            chapter.setId(1L);
        }

        @Test
        void shouldCreateStoryProgress_whenNotFound() {
            Story story = Story.builder()
                    .id(1L).build();
            StoryProgress progress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(null);
            when(storyRepository.findById(story.getId())).thenReturn(Optional.of(story));
            when(playerService.getPlayerById(request.getPlayerId())).thenReturn(player);
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(storyService.ensureStoryProgress(request.getPlayerId(), request.getStoryId())).thenReturn(progress);
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));

            chapterService.startChapter(request);
            
            ArgumentCaptor<StoryProgress> captor = ArgumentCaptor.forClass(StoryProgress.class);
            verify(storyProgressRepository).save(captor.capture());

            StoryProgress newStoryProgress = captor.getValue();

            assertAll(
                    "Story progress should be properly updated",
                    () -> assertEquals(ProgressStatus.IN_PROGRESS, newStoryProgress.getProgressStatus()),
                    () -> assertEquals(LocalDate.now(), newStoryProgress.getStartedAt()),
                    () -> assertEquals(chapter.getId(), newStoryProgress.getCurrentChapterId())
            );
        }
        
        @Test
        void shouldUpdateStoryProgress_whenStartingChapterFirstTime() {

            StoryProgress existingProgress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(existingProgress);
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
            
            chapterService.startChapter(request);

            ArgumentCaptor<StoryProgress> captor = ArgumentCaptor.forClass(StoryProgress.class);
            verify(storyProgressRepository).save(captor.capture());
            
            StoryProgress newStoryProgress = captor.getValue();

            assertAll(
                    "Story progress should be properly updated",
                    () -> assertEquals(ProgressStatus.IN_PROGRESS, newStoryProgress.getProgressStatus()),
                    () -> assertEquals(LocalDate.now(), newStoryProgress.getStartedAt()),
                    () -> assertEquals(chapter.getId(), newStoryProgress.getCurrentChapterId())
            );
        }

        @Test
        void shouldUpdateLastPlayed_whenReturningToInProgressChapter() {
            LocalDate previousStart = LocalDate.now().minusDays(1);
            StoryProgress existingProgress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .startedAt(previousStart)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(existingProgress);
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chapterRepository.findById(chapter.getId()))
                    .thenReturn(Optional.of(chapter));
            
            chapterService.startChapter(request);
            
            ArgumentCaptor<StoryProgress> captor = ArgumentCaptor.forClass(StoryProgress.class);
            verify(storyProgressRepository).save(captor.capture());

            StoryProgress savedProgress = captor.getValue();
            assertAll(
                    "Story progress should only update lastPlayed",
                    () -> assertEquals(ProgressStatus.IN_PROGRESS, savedProgress.getProgressStatus()),
                    () -> assertEquals(previousStart, savedProgress.getStartedAt()),
                    () -> assertEquals(LocalDate.now(), savedProgress.getLastPlayed())
            );
        }

        @Test
        void shouldCreateNewChapterProgress_whenStartingChapterFirstTime() {
            StoryProgress existingProgress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(existingProgress);
            when(chapterRepository.findById(chapter.getId()))
                    .thenReturn(Optional.of(chapter));
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chapterProgressRepository.findByStoryProgressIdAndChapterId(existingProgress.getId(), chapter.getId()))
                    .thenReturn(null);
            
            chapterService.startChapter(request);
            
            ArgumentCaptor<ChapterProgress> captor = ArgumentCaptor.forClass(ChapterProgress.class);
            verify(chapterProgressRepository).save(captor.capture());

            ChapterProgress savedProgress = captor.getValue();
            assertAll(
                    "Chapter progress should be properly created",
                    () -> assertEquals(existingProgress, savedProgress.getStoryProgress()),
                    () -> assertEquals(chapter, savedProgress.getChapter()),
                    () -> assertEquals(LocalDate.now(), savedProgress.getStartedAt()),
                    () -> assertEquals(ProgressStatus.IN_PROGRESS, savedProgress.getProgressStatus())
            );
        }

        @Test
        void shouldNotCreateChapterProgress_whenAlreadyExists() {
            StoryProgress existingStoryProgress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.IN_PROGRESS)
                    .build();

            ChapterProgress existingChapterProgress = ChapterProgress.builder()
                    .storyProgress(existingStoryProgress)
                    .chapter(chapter)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(existingStoryProgress);
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chapterRepository.findById(chapter.getId()))
                    .thenReturn(Optional.of(chapter));
            when(chapterProgressRepository.findByStoryProgressIdAndChapterId(existingStoryProgress.getId(), chapter.getId()))
                    .thenReturn(existingChapterProgress);
            
            chapterService.startChapter(request);
            
            verify(chapterProgressRepository, never()).save(any());
        }

        @Test
        void shouldInitializeChapterSession_whenStartingChapter() {
            StoryProgress existingStoryProgress = StoryProgress.builder()
                    .progressStatus(ProgressStatus.NOT_STARTED)
                    .build();

            when(storyProgressRepository.findByPlayerIdAndStoryId(request.getPlayerId(), request.getStoryId()))
                    .thenReturn(existingStoryProgress);
            when(storyProgressRepository.save(any(StoryProgress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chapterRepository.findById(chapter.getId()))
                    .thenReturn(Optional.of(chapter));
            
            chapterService.startChapter(request);
            
            verify(chapterSessionService).initializeChapterSession(request.getPlayerId(), chapter);
        }
    }
}
