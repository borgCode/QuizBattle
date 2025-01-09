package org.borg.backend.game.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.service.GameService;
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
    public List<MultiplayerSessionDTO> getPlayerSessions(@PathVariable long playerId) {
        return gameService.getMultiplayerSessionsById(playerId);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable long sessionId) {
        return ResponseEntity.ok(gameService.getGameState(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/players/{playerId}/acknowledge")
    public ResponseEntity<Void> acknowledgeGameOver(@PathVariable long sessionId, @PathVariable long playerId) {
   
        gameService.acknowledgeGameOver(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/players/{playerId}/forfeit")
    public ResponseEntity<Void> giveUp(@PathVariable long sessionId, @PathVariable long playerId) {

        gameService.handleGiveUp(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
    
    
}
