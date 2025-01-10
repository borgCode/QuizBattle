package org.borg.backend.game.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.game.multiplayer.dto.MatchRequest;
import org.borg.backend.game.multiplayer.dto.RematchRequest;
import org.borg.backend.game.multiplayer.dto.MatchResponse;
import org.borg.backend.game.multiplayer.service.PlayerMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("multiplayer/match")
@RequiredArgsConstructor
@Tag(name = "Multiplayer match")
public class PlayerMatchController {

    private final PlayerMatchService playerMatchService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#matchRequest.senderId)")
    @PostMapping()
    public ResponseEntity<Void> requestMatch(@RequestBody MatchRequest matchRequest) {
        playerMatchService.requestMatch(matchRequest);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#rematchRequest.playerId)")
    @PostMapping("/rematch")
    public ResponseEntity<Void> requestRematch(@RequestBody RematchRequest rematchRequest) {
        playerMatchService.requestRematch(rematchRequest);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#response.senderId)")
    @PostMapping("/accept")
    public ResponseEntity<Long> acceptMatch(@RequestBody MatchResponse response) {
        return ResponseEntity.ok(playerMatchService.handleMatchAccept(response));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#response.senderId)")
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectMatch(@RequestBody MatchResponse response) {
        playerMatchService.handleMatchReject(response);
        return ResponseEntity.ok().build();
    }
}
