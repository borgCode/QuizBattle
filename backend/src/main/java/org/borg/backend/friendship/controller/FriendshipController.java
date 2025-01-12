package org.borg.backend.friendship.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.friendship.model.FriendshipStatus;
import org.borg.backend.friendship.dto.RelationshipsDTO;
import org.borg.backend.friendship.service.FriendshipService;
import org.borg.backend.friendship.dto.PlayerInteraction;
import org.borg.backend.friendship.dto.PlayerInteractionResponse;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.friendship.dto.RelationshipStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("friendship")
@RequiredArgsConstructor
@Tag(name = "Friendship")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/add")
    public ResponseEntity<Void> addFriend(@RequestBody PlayerInteraction request) {
        friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#response.senderId)")
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptFriend(@RequestBody PlayerInteractionResponse response) {
        friendshipService.handleFriendshipResponse(response, true);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#response.senderId)")
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectFriendship(@RequestBody PlayerInteractionResponse response) {
        friendshipService.handleFriendshipResponse(response, false);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/block")
    public ResponseEntity<Void> blockPlayer(@RequestBody PlayerInteraction request) {
        friendshipService.blockPlayer(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/unblock")
    public ResponseEntity<Void> unblockPlayer(@RequestBody PlayerInteraction request) {
        friendshipService.unblockPlayer(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/remove")
    public ResponseEntity<Void> removeAsFriend(@RequestBody PlayerInteraction request) {
        friendshipService.removeAsFriend(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/friends/{playerId}")
    public ResponseEntity<List<PlayerDTO>> getFriends(@PathVariable long playerId) {
        return ResponseEntity.ok(friendshipService.getFriends(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/relationships/{playerId}")
    public ResponseEntity<RelationshipsDTO> getRelationships(@PathVariable long playerId) {
        return ResponseEntity.ok(friendshipService.getRelationships(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#relationshipStatusRequest.playerId)")
    @GetMapping("/relationship/status")
    public ResponseEntity<FriendshipStatus> getRelationshipStatus(RelationshipStatusRequest relationshipStatusRequest) {
        return ResponseEntity.ok(friendshipService.getRelationshipStatus(relationshipStatusRequest));
    }
}
