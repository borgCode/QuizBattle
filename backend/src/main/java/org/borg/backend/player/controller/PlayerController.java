package org.borg.backend.player.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.model.UpdatePlayerRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("player")
@RequiredArgsConstructor
@Tag(name="Player")
public class PlayerController {
    
    private final PlayerService playerService;

    @GetMapping("/{playerId}")
    public PlayerDTO getPlayerById(@PathVariable Long playerId) {
        return playerService.getPlayerById(playerId);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updatePlayer(@RequestBody UpdatePlayerRequest request) {
        playerService.updatePlayer(request);
        return ResponseEntity.ok().build();  
    }
    
    @PostMapping(value = "/{playerId}/profile-picture-upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Long playerId, @Parameter() @RequestPart("file") MultipartFile file) {
        playerService.uploadProfilePicture(playerId, file);
        return ResponseEntity.accepted().build();
    }
    
    
    
    //TODO add friend mapping
    
    //TODO send message mapping
}
   

