package org.borg.backend.social.friendship.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.social.block.service.PlayerBlockService;
import org.borg.backend.social.friendship.dto.*;
import org.borg.backend.social.friendship.model.FriendshipStatus;
import org.borg.backend.social.friendship.service.FriendshipService;
import org.borg.backend.player.dto.PlayerDTO;
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
    private final PlayerBlockService playerBlockService;

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
        return ResponseEntity.ok(RelationshipsDTO.builder()
                .friends(friendshipService.getFriends(playerId))
                .blocked(playerBlockService.getBlocked(playerId))
                .build());
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#relationshipStatusRequest.playerId)")
    @GetMapping("/relationship/status")
    public ResponseEntity<RelationshipStatus> getRelationshipStatus(RelationshipStatusRequest relationshipStatusRequest) {
        if (playerBlockService.checkBlockForRelationshipStatus(relationshipStatusRequest)) {
            return ResponseEntity.ok(new RelationshipStatus(null, true));
        }
        
        FriendshipStatus friendshipStatus = friendshipService.getFriendshipStatus(relationshipStatusRequest);
        return ResponseEntity.ok(new RelationshipStatus(friendshipStatus, false));
    }
}
