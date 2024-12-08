package org.borg.backend.friendship;


import lombok.RequiredArgsConstructor;
import org.borg.backend.player.PlayerDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("friendship")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/add")
    public ResponseEntity<String> addFriend(@RequestBody PlayerInteraction request) {
        friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok("Friend request sent");
    }
    @PostMapping("/block")
    public ResponseEntity<String> blockPlayer(@RequestBody PlayerInteraction request) {
        friendshipService.blockPlayer(request);
        return ResponseEntity.ok("Player blocked");
    }
    
    @GetMapping("/friends/{playerId}")
    public ResponseEntity<List<PlayerDTO>> getFriends(@PathVariable Long playerId) {
        return ResponseEntity.ok(friendshipService.getFriends(playerId));
    }
}
