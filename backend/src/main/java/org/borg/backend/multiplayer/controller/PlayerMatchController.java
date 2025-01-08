package org.borg.backend.multiplayer.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.dto.RematchResponse;
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

    @PostMapping("/sessions/rematch")
    public ResponseEntity<Void> requestRematch(@RequestBody RematchRequest rematchRequest) {
        playerMatchService.requestRematch(rematchRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/accept")
    public ResponseEntity<Long> acceptRematch(@RequestBody RematchResponse response) {
        return ResponseEntity.ok(playerMatchService.handleRematchAccept(response));
    }

    @PostMapping("/sessions/reject")
    public ResponseEntity<Void> rejectRematch(@RequestBody RematchResponse response) {
        playerMatchService.handleRematchReject(response);
        return ResponseEntity.ok().build();
    }
}
