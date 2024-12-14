package org.borg.backend.friendship;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.friendship.model.PlayerInteraction;
import org.borg.backend.friendship.model.PlayerInteractionResponse;
import org.borg.backend.player.model.PlayerDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("friendship")
@RequiredArgsConstructor
@Tag(name = "Friendship")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/add")
    public ResponseEntity<Void> addFriend(@RequestBody PlayerInteraction request) {
        friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptFriend(@RequestBody PlayerInteractionResponse response) {
        friendshipService.handleFriendshipResponse(response, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectFriendship(@RequestBody PlayerInteractionResponse response) {
        friendshipService.handleFriendshipResponse(response, false);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/block")
    public ResponseEntity<Void> blockPlayer(@RequestBody PlayerInteraction request) {
        friendshipService.blockPlayer(request);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/friends/{playerId}")
    public ResponseEntity<List<PlayerDTO>> getFriends(@PathVariable Long playerId) {
        return ResponseEntity.ok(friendshipService.getFriends(playerId));
    }
}
