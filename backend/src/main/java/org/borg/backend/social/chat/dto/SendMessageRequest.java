package org.borg.backend.social.chat.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Message content cannot be empty")
    private String message;
}
