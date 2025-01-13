package org.borg.backend.social.block.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BlockRequest {
    private Long senderId;
    private Long receiverId;
}
