package org.borg.backend.chat.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConversationRequest {
    private Long senderId;
    private Long receiverId;
    
}
