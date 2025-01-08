package org.borg.backend.multiplayer.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.dto.*;
import org.borg.backend.multiplayer.service.GameService;
import org.borg.backend.multiplayer.service.MatchMakingService;
import org.borg.backend.multiplayer.service.PlayerMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("multiplayer")
@RequiredArgsConstructor
@Tag(name = "Multiplayer")
public class MultiplayerController {
    private final MatchMakingService matchMakingService;
    private final GameService gameService;
    private final PlayerMatchService playerMatchService;

    @GetMapping("{playerId}")
    public List<MultiplayerSessionDTO> getPlayerSessions(@PathVariable Long playerId) {
        return gameService.getMultiplayerSessionsById(playerId);
    }

    @MessageMapping("/matchmaking/find")
    public void findMatch(long playerId) {
        matchMakingService.findMatch(playerId);
    }

    @MessageMapping("/matchmaking/accept")
    public void acceptMatch(@Payload MatchDecision matchDecision) {
        matchMakingService.handleMatchResponse(matchDecision.getMatchmakingSessionId(), matchDecision.getPlayerId(), true);
    }

    @MessageMapping("/matchmaking/decline")
    public void declineMatch(@Payload MatchDecision matchDecision) {
        matchMakingService.handleMatchResponse(matchDecision.getMatchmakingSessionId(), matchDecision.getPlayerId(), false);
    }


    @DeleteMapping("/matchmaking/cancel/{playerId}")
    public ResponseEntity<Void> cancelMatchmaking(@PathVariable Long playerId) {
        matchMakingService.cancelMatchmaking(playerId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/session/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.getGameState(sessionId));
    }

    @PostMapping("/session/rematch-request")
    public ResponseEntity<Void> requestRematch(@RequestBody RematchRequest rematchRequest) {
        playerMatchService.requestRematch(rematchRequest);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/session/accept")
    public ResponseEntity<Long> acceptRematch(@RequestBody RematchResponse response) {
        return ResponseEntity.ok(playerMatchService.handleRematchAccept(response));
    }

    @PostMapping("/session/reject")
    public ResponseEntity<Void> rejectRematch(@RequestBody RematchResponse response) {
        playerMatchService.handleRematchReject(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/complete/{playerId}")
    public ResponseEntity<Void> acknowledgeGameOver(@PathVariable Long sessionId, @PathVariable Long playerId) {
        log.warn("Called complete game");
        gameService.acknowledgeGameOver(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{sessionId}/complete/{playerId}")
    public ResponseEntity<Void> giveUp(@PathVariable Long sessionId, @PathVariable Long playerId) {
        log.warn("Giving up game");
        gameService.handleGiveUp(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    

}
