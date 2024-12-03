package org.borg.backend.multiplayer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("multiplayer")
@RequiredArgsConstructor
public class MultiplayerController {
    private final MultiplayerService multiplayerService;
    
    @PostMapping("/matchmaking")
    public ResponseEntity<MatchmakingResponse> findMatch(
            @RequestBody MatchmakingRequest request) {
        return ResponseEntity.ok(multiplayerService.findMatch(request));
    }
    
    @DeleteMapping("/matchmaking/{playerId}")
    public ResponseEntity<Void> cancelMatchmaking(@PathVariable Long playerId) {
        multiplayerService.cancelMatchmaking(playerId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(multiplayerService.getGameState(sessionId));
    }
    
    
}
