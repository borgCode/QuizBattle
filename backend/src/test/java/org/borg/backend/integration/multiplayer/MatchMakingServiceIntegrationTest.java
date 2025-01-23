package org.borg.backend.integration.multiplayer;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.game.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.game.multiplayer.model.MatchmakingSession;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MatchmakingSessionRepository;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.service.MatchMakingService;
import org.borg.backend.game.multiplayer.service.SessionCleanUpService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MatchMakingServiceIntegrationTest {
    @Autowired
    private MatchMakingService matchMakingService;
    @Autowired
    private MultiplayerSessionRepository multiplayerSessionRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private MatchmakingSessionRepository matchmakingSessionRepository;
    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private SessionCleanUpService sessionCleanUpService;
    @Autowired
    private PlayerBlockService playerBlockService;

    @BeforeEach
    void setUp() {

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @AfterEach
    void tearDown() {
        matchMakingService.clearQueue();
        multiplayerSessionRepository.deleteAll();
        playerRepository.deleteAll();
        matchmakingSessionRepository.deleteAll();
    }

    @Nested
    class MatchingMakingFullFLowTests {
        Player player1;
        Player player2;

        @BeforeEach
        void setUp() {
            player1 = createAndSavePlayer("Player1");
            player2 = createAndSavePlayer("Player2");
        }

        @Test
        void fullFlowMatchTwoPlayers_bothAcceptAndCreateMultiplayerSession() {
            MatchmakingSession matchmakingSession = setupMatchmakingSession();

            matchMakingService.handleMatchResponse(matchmakingSession.getId(), player1.getId(), true);

            Pageable pageable = PageRequest.of(0, 10);
            assertTrue(multiplayerSessionRepository.findByPlayerId(player1.getId(), pageable).isEmpty(), "Multiplayer session should not be created until both players accept");

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.waitingForOtherPlayer());

            matchMakingService.handleMatchResponse(matchmakingSession.getId(), player2.getId(), true);

            
            Page<MultiplayerSession> player1Sessions = multiplayerSessionRepository.findByPlayerId(player1.getId(), pageable);
            Page<MultiplayerSession> player2Sessions = multiplayerSessionRepository.findByPlayerId(player2.getId(), pageable);

            assertAll("Post both accept match checks",
                    () -> assertEquals(1, player1Sessions.getTotalElements(), "Player1 should have one active game"),
                    () -> assertEquals(1, player2Sessions.getTotalElements(), "Player2 should have one active game"),
                    () -> assertEquals(player1Sessions.getContent().get(0).getId(),
                            player2Sessions.getContent().get(0).getId(),
                            "Both players should have the same session ID")
            );

            verifyMatchmakingResponse(player1.getId(),
                    MatchmakingResponse.accepted(player1Sessions.getContent().get(0).getId(),
                            player2.getDisplayName()));
            verifyMatchmakingResponse(player2.getId(),
                    MatchmakingResponse.accepted(player2Sessions.getContent().get(0).getId(),
                            player1.getDisplayName()));

            MatchmakingSession postAcceptSession = matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player1.getId(), player2.getId()
                    );
            assertNull(postAcceptSession, "Matchmaking session should be cleaned up after both players accepted");
        }

        @Test
        void blockedPlayersShouldNotBeMatchedTogether() {
            Player player3 = createAndSavePlayer("Player3");

            playerBlockService.blockPlayer(player1.getId(), player2.getId());

            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "Player1 should be in queue");

            matchMakingService.findMatch(player2.getId());
            assertEquals(2, matchMakingService.getQueueSize(), "Players should not be matched due to block");

            matchMakingService.findMatch(player3.getId());

            assertNull(matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player1.getId(), player2.getId()
                    ));

            MatchmakingSession player3WithPlayer1 = matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player1.getId(), player3.getId()
                    );

            MatchmakingSession player3WithPlayer2 = matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player2.getId(), player3.getId()
                    );

            assertTrue(player3WithPlayer1 != null || player3WithPlayer2 != null,
                    "Player3 should be matched with either player1 or player2");

            assertEquals(1, matchMakingService.getQueueSize(), "There should only be one player left in the queue");
        }

        @Test
        void twoPlayersMatched_oneDeclineAndCleanUp() {
            MatchmakingSession savedMatchmakingSession = setupMatchmakingSession();

            matchMakingService.handleMatchResponse(savedMatchmakingSession.getId(), player1.getId(), false);

            verifyMatchmakingResponse(player2.getId(), MatchmakingResponse.declined());

            MatchmakingSession postDeclinedSession = matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player1.getId(), player2.getId()
                    );
            assertNull(postDeclinedSession, "Matchmaking session should be cleaned up after both players after someone declined");
        }

        @Test
        void TwoPlayersMatched_oneTimedOutAndCleanUp() {
            MatchmakingSession savedMatchmakingSession = setupMatchmakingSession();
            matchMakingService.handleMatchResponse(savedMatchmakingSession.getId(), player1.getId(), true);

            Instant futureTime = savedMatchmakingSession.getCreatedAt().plusSeconds(16);
            sessionCleanUpService.cleanUpMatchmakingSessions(futureTime);

            assertFalse(matchmakingSessionRepository.existsById(savedMatchmakingSession.getId()));
        }

        private MatchmakingSession setupMatchmakingSession() {
            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "There should be one person in the queue");

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.waiting());

            matchMakingService.findMatch(player2.getId());
            assertEquals(0, matchMakingService.getQueueSize(), "There should be 0 players in the queue");

            MatchmakingSession savedMatchmakingSession = matchmakingSessionRepository
                    .findMatchmakingSessionByPlayerIds(
                            player1.getId(), player2.getId()
                    );

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.matched(savedMatchmakingSession.getId(), player2.getDisplayName()));
            verifyMatchmakingResponse(player2.getId(), MatchmakingResponse.matched(savedMatchmakingSession.getId(), player1.getDisplayName()));

            return savedMatchmakingSession;
        }

        private void verifyMatchmakingResponse(Long playerId, MatchmakingResponse expectedResponse) {
            verify(simpMessagingTemplate).convertAndSend(
                    eq("/topic/match" + playerId),
                    (MatchmakingResponse) argThat(response ->
                            response.equals(expectedResponse))
            );
        }

        @Test
        void testPlayerCancelMatchmaking() {
            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "There should be one person in the queue");

            matchMakingService.cancelMatchmaking(player1.getId());
            assertEquals(0, matchMakingService.getQueueSize(), "There should be no players in the queue");
        }

        @Test
        void playersShouldNotMatchWithThemselves() {
            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "There should be one person in the queue");

            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "There should be one person in the queue");
        }
    }

    @Test
    void concurrentMatchmakingTest() throws InterruptedException {
        int numOfPlayers = 100;
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < numOfPlayers; i++) {
            players.add(createAndSavePlayer("Player " + i));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numOfPlayers);

        for (Player player : players) {
            executorService.submit(() -> {
                try {
                    matchMakingService.findMatch(player.getId());
                } finally {
                    latch.countDown();
                }
            });
        }
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Not all matchmaking operations finished on time");

        executorService.shutdown();

        List<MatchmakingSession> matchmakingSessions = matchmakingSessionRepository.findAll();

        assertAll(
                () -> assertEquals(matchmakingSessions.size() * 2, players.size(),
                        "Mismatch in players in Matchmaking sessions"),
                () -> assertEquals(0, matchMakingService.getQueueSize(),
                        "Mismatch in players in the queue")
        );
    }

    private Player createAndSavePlayer(String name) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username(name.toLowerCase())
                .password("password")
                .displayName(name)
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        return playerRepository.save(player);
    }
}