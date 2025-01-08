package org.borg.backend.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.dto.MatchRequest;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.dto.MatchResponse;
import org.borg.backend.multiplayer.service.PlayerMatchService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping()
    public ResponseEntity<Void> requestMatch(@RequestBody MatchRequest matchRequest) {
        playerMatchService.requestMatch(matchRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rematch")
    public ResponseEntity<Void> requestRematch(@RequestBody RematchRequest rematchRequest) {
        playerMatchService.requestRematch(rematchRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept")
    public ResponseEntity<Long> acceptMatch(@RequestBody MatchResponse response) {
        return ResponseEntity.ok(playerMatchService.handleMatchAccept(response));
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectMatch(@RequestBody MatchResponse response) {
        playerMatchService.handleMatchReject(response);
        return ResponseEntity.ok().build();
    }
}
