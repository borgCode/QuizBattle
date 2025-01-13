package org.borg.backend.social.chat.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class MessageDTO {
    private Long id;
    private Long senderId;
    private Instant sentAt;
    private boolean isRead;
    private String content;
}
