package org.borg.backend.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String receiverUsername;
    private Long conversationId;
    private String message;
}
