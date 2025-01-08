package org.borg.backend.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.dto.GameStateResponse;
import org.borg.backend.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.multiplayer.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("multiplayer/game")
@RequiredArgsConstructor
@Tag(name = "Multiplayer Game")
public class GameController {

    private final GameService gameService;

    @GetMapping("players/{playerId}/sessions")
    public List<MultiplayerSessionDTO> getPlayerSessions(@PathVariable Long playerId) {
        return gameService.getMultiplayerSessionsById(playerId);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.getGameState(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/players/{playerId}/acknowledge")
    public ResponseEntity<Void> acknowledgeGameOver(@PathVariable Long sessionId, @PathVariable Long playerId) {
   
        gameService.acknowledgeGameOver(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/players/{playerId}/forfeit")
    public ResponseEntity<Void> giveUp(@PathVariable Long sessionId, @PathVariable Long playerId) {

        gameService.handleGiveUp(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    
}
