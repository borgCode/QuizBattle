package org.borg.backend.unit.game.multiplayer;

import org.borg.backend.game.multiplayer.dto.MatchmakingResponse;
import org.borg.backend.game.multiplayer.service.MatchMakingService;
import org.borg.backend.player.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class MatchMakingServiceTest {
    
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @InjectMocks
    private MatchMakingService matchMakingService;
    
    private Player player1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player1 = Player.builder()
                .id(1L)
                .displayName("Player 1")
                .build();

        matchMakingService.clearQueue();
    }

    @Test
    void shouldHandleRapidFindAndCancel() {
        for (int i = 0; i < 10; i++) {
            matchMakingService.findMatch(player1.getId());
            matchMakingService.cancelMatchmaking(player1.getId());
        }

        assertEquals(0, matchMakingService.getQueueSize());

        verify(simpMessagingTemplate, times(10)).convertAndSend(
                eq("/topic/match" + player1.getId()),
                any(MatchmakingResponse.class));
    }
}
