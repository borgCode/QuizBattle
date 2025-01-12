package org.borg.backend.player.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.dto.UpdatePlayerRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("player")
@RequiredArgsConstructor
@Tag(name="Player")
public class PlayerController {
    
    private final PlayerService playerService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/{playerId}")
    public PlayerDTO getPlayerById(@PathVariable long playerId) {
        return playerService.getPlayerDTOById(playerId);
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PutMapping("/update")
    public ResponseEntity<Void> updatePlayer(@RequestBody UpdatePlayerRequest request) {
        playerService.updatePlayer(request);
        return ResponseEntity.ok().build();  
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping(value = "/{playerId}/profile-picture-upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable long playerId, @Parameter() @RequestPart("file") MultipartFile file) {
        playerService.uploadProfilePicture(playerId, file);
        return ResponseEntity.accepted().build();
    }
}
   

