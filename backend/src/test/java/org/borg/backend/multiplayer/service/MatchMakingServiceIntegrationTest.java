package org.borg.backend.multiplayer.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private PendingSessionRepository pendingSessionRepository;
    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private SessionCleanUpService sessionCleanUpService;

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
        pendingSessionRepository.deleteAll();
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
            PendingSession savedPendingSession = setupPendingSession();

            matchMakingService.handleMatchResponse(savedPendingSession.getId(), player1.getId(), true);
            assertTrue(multiplayerSessionRepository.findByPlayerId(player1.getId()).isEmpty(), "Multiplayer session should not be created until both players accept");

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.waitingForOtherPlayer());

            matchMakingService.handleMatchResponse(savedPendingSession.getId(), player2.getId(), true);
            
            List<MultiplayerSession> player1Sessions = multiplayerSessionRepository.findByPlayerId(player1.getId());
            List<MultiplayerSession> player2Sessions = multiplayerSessionRepository.findByPlayerId(player2.getId());
            assertAll("Post both accept match checks",
                    () -> assertEquals(1, player1Sessions.size(), "Player1 should have one active game"),
                    () -> assertEquals(1, player2Sessions.size(), "Player2 should have one active game"),
                    () -> assertEquals(player1Sessions.get(0).getId(), player2Sessions.get(0).getId(), "Both players should have the same session ID")
            );
            
            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.accepted(player1Sessions.get(0).getId(), player2.getDisplayName()));
            verifyMatchmakingResponse(player2.getId(), MatchmakingResponse.accepted(player1Sessions.get(0).getId(), player1.getDisplayName()));
            
            PendingSession postAcceptPendingSession = pendingSessionRepository
                    .findByRequestingPlayerIdAndOpponentIdOrRequestingPlayerIdAndOpponentId(
                            player1.getId(), player2.getId(),
                            player2.getId(), player1.getId()
                    );
            assertNull(postAcceptPendingSession, "Pending session should be cleaned up after both players accepted");
            
        }
        
        @Test
        void twoPlayersMatched_oneDeclineAndCleanUp() {
            PendingSession savedPendingSession = setupPendingSession();

            matchMakingService.handleMatchResponse(savedPendingSession.getId(), player1.getId(), false);
            
            verifyMatchmakingResponse(player2.getId(), MatchmakingResponse.declined());

            PendingSession postDeclinedPendingSession = pendingSessionRepository
                    .findByRequestingPlayerIdAndOpponentIdOrRequestingPlayerIdAndOpponentId(
                            player1.getId(), player2.getId(),
                            player2.getId(), player1.getId()
                    );
            assertNull(postDeclinedPendingSession, "Pending session should be cleaned up after both players after someone declined");
            
        }
        
        @Test
        void TwoPlayersMatched_oneTimedOutAndCleanUp() {
            PendingSession savedPendingSession = setupPendingSession();
            matchMakingService.handleMatchResponse(savedPendingSession.getId(), player1.getId(), true);
            
            Instant futureTime = savedPendingSession.getCreatedAt().plusSeconds(16);
            sessionCleanUpService.cleanUpPendingSessions(futureTime);
            
            assertFalse(pendingSessionRepository.existsById(savedPendingSession.getId()));
            
        }
        private PendingSession setupPendingSession() {
            matchMakingService.findMatch(player1.getId());
            assertEquals(1, matchMakingService.getQueueSize(), "There should be one person in the queue");

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.waiting());

            matchMakingService.findMatch(player2.getId());
            assertEquals(0, matchMakingService.getQueueSize(), "There should be 0 players in the queue");

            PendingSession savedPendingSession = pendingSessionRepository
                    .findByRequestingPlayerIdAndOpponentIdOrRequestingPlayerIdAndOpponentId(
                            player1.getId(), player2.getId(),
                            player2.getId(), player1.getId()
                    );

            verifyMatchmakingResponse(player1.getId(), MatchmakingResponse.matched(savedPendingSession.getId(), player2.getDisplayName()));
            verifyMatchmakingResponse(player2.getId(), MatchmakingResponse.matched(savedPendingSession.getId(), player1.getDisplayName()));
            
            return savedPendingSession;
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
        void playersShouldNotMatchWithThemselves () {
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

        List<PendingSession> pendingSessions = pendingSessionRepository.findAll();

        assertAll(
                () -> assertEquals(pendingSessions.size() * 2, players.size(),
                        "Mismatch in players in pending sessions"),
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