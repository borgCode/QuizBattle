package org.borg.backend.multiplayer.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.model.GameStateResponse;
import org.borg.backend.multiplayer.model.MatchDecision;
import org.borg.backend.multiplayer.model.RematchResponse;
import org.borg.backend.multiplayer.service.MatchMakingService;
import org.borg.backend.multiplayer.service.MultiplayerService;
import org.borg.backend.multiplayer.model.MultiplayerSessionDTO;
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
    private final MultiplayerService multiplayerService;
    private final MatchMakingService matchMakingService;

    @GetMapping("{playerId}")
    public List<MultiplayerSessionDTO> getPlayerSessions(@PathVariable Long playerId) {
        return multiplayerService.getMultiplayerSessionsById(playerId);
    }

    @MessageMapping("/matchmaking/find")
    public void findMatch(long playerId) {
        log.warn("Receiving matchmaking request for player: {}", playerId);
        matchMakingService.findMatch(playerId);
    }

    @MessageMapping("/matchmaking/accept")
    public void acceptMatch(@Payload MatchDecision matchDecision) {
        log.warn("{}: has accepted session: {}", matchDecision.getPlayerId(), matchDecision.getPendingSessionId());
        matchMakingService.handleMatchResponse(matchDecision.getPendingSessionId(), matchDecision.getPlayerId(), true);
    }

    @MessageMapping("/matchmaking/decline")
    public void declineMatch(@Payload MatchDecision matchDecision) {
        log.warn("{}: has declined session: {}", matchDecision.getPlayerId(), matchDecision.getPendingSessionId());
        matchMakingService.handleMatchResponse(matchDecision.getPendingSessionId(), matchDecision.getPlayerId(), false);
    }


    @DeleteMapping("/matchmaking/cancel/{playerId}")
    public ResponseEntity<Void> cancelMatchmaking(@PathVariable Long playerId) {
        matchMakingService.cancelMatchmaking(playerId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/session/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(multiplayerService.getGameState(sessionId));
    }

    @PostMapping("/session/{sessionId}/rematch-request/{playerId}")
    public ResponseEntity<Void> requestRematch(@PathVariable Long sessionId, @PathVariable Long playerId) {
        multiplayerService.requestRematch(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/session/accept")
    public ResponseEntity<Long> acceptRematch(@RequestBody RematchResponse response) {
        return ResponseEntity.ok(multiplayerService.handleRematchAccept(response));
    }

    @PostMapping("/session/reject")
    public ResponseEntity<Void> rejectRematch(@RequestBody RematchResponse response) {
        multiplayerService.handleRematchReject(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/complete/{playerId}")
    public ResponseEntity<Void> acknowledgeGameOver(@PathVariable Long sessionId, @PathVariable Long playerId) {
        log.warn("Called complete game");
        multiplayerService.acknowledgeGameOver(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{sessionId}/complete/{playerId}")
    public ResponseEntity<Void> giveUp(@PathVariable Long sessionId, @PathVariable Long playerId) {
        log.warn("Giving up game");
        multiplayerService.handleGiveUp(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    

}
