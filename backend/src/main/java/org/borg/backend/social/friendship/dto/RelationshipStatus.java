package org.borg.backend.social.friendship.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.social.friendship.model.FriendshipStatus;


@Getter
@Setter
@AllArgsConstructor
public class RelationshipStatus {
    private FriendshipStatus friendshipStatus;
    private boolean isBlocked;
}