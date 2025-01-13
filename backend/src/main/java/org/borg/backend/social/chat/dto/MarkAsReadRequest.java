package org.borg.backend.social.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MarkAsReadRequest {
    private List<Long> messageIds;
    private Long conversationId;
    private Long playerId;
    private String username;
}
