package org.borg.backend.multiplayer;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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

    @PostMapping("/matchmaking/find/{playerId}")
    public ResponseEntity<Void> findMatch(@PathVariable Long playerId) {
        log.warn("Receiving matchmaking request");
        multiplayerService.findMatch(playerId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/matchmaking/cancel/{playerId}")
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
