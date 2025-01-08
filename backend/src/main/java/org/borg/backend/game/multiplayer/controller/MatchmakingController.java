package org.borg.backend.game.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.game.multiplayer.dto.MatchDecision;
import org.borg.backend.game.multiplayer.service.MatchMakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("multiplayer/matchmaking")
@RequiredArgsConstructor
@Tag(name = "Multiplayer Matchmaking")
public class MatchmakingController {

    private final MatchMakingService matchMakingService;

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
}
