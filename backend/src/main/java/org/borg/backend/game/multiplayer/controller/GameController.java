package org.borg.backend.game.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.service.GameService;
import org.borg.backend.game.multiplayer.service.MultiplayerSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("multiplayer/game")
@RequiredArgsConstructor
@Tag(name = "Multiplayer Game")
public class GameController {

    private final GameService gameService;
    private final MultiplayerSessionService multiplayerSessionService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("players/{playerId}/sessions")
    public Page<MultiplayerSessionDTO> getPlayerSessions(@PathVariable long playerId, @PageableDefault (size = 20) Pageable pageable) {
        return multiplayerSessionService.getMultiplayerSessionsById(playerId, pageable);
    }
    
    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable long sessionId, @RequestParam long playerId) {
        return ResponseEntity.ok(gameService.getGameState(sessionId, playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/sessions/{sessionId}/players/{playerId}/acknowledge")
    public ResponseEntity<Void> acknowledgeGameOver(@PathVariable long sessionId, @PathVariable long playerId) {
   
        gameService.acknowledgeGameOver(sessionId, playerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/sessions/{sessionId}/players/{playerId}/forfeit")
    public ResponseEntity<Void> giveUp(@PathVariable long sessionId, @PathVariable long playerId) {

        gameService.handleGiveUp(sessionId, playerId);
        return ResponseEntity.ok().build();
    }
}
