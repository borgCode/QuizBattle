package org.borg.backend.multiplayer;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("multiplayer")
@RequiredArgsConstructor
@Tag(name = "Multiplayer")
public class MultiplayerController {
    private final MultiplayerService multiplayerService;
    
    @GetMapping("{playerId}")
    public List<MultiplayerSessionDTO> getPlayerSessions(@PathVariable Long playerId) {
        return multiplayerService.getMultiplayerSessionsById(playerId);
    }
    @GetMapping()
    
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
    
    
    
    //TODO Give up mapping
    
    
    
    
}
