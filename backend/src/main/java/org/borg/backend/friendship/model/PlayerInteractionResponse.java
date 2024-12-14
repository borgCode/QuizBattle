package org.borg.backend.friendship.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlayerInteractionResponse {
    private Long senderId;
    private Long receiverId;
    private Long notificationId;
}
