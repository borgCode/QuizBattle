package org.borg.backend.social.friendship.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlayerInteraction {
    private Long senderId;
    private Long receiverId;
}
