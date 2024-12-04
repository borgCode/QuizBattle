package org.borg.backend.player;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("player")
@RequiredArgsConstructor
@Tag(name="Player")
public class PlayerController {
    
    private final PlayerService playerService;

    @GetMapping("/{username}")
    public PlayerDTO getPlayerById(@PathVariable String username) {
        return playerService.getPlayerByName(username);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updatePlayer(@RequestBody UpdatePlayerRequest request) {
        playerService.updatePlayer(request);
        return ResponseEntity.ok().build();  
    }
    
    //TODO add friend mapping
    
    //TODO send message mapping
}
   

