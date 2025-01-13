package org.borg.backend.social.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.social.chat.dto.*;
import org.borg.backend.social.chat.service.MessagingService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("messages")
@RequiredArgsConstructor
@Tag(name = "Message")
public class MessageController {


    private final MessagingService messagingService;

    @MessageMapping("/messages/send")
    public void sendMessage(@Valid @Payload SendMessageRequest messageRequest) {
        messagingService.sendMessage(messageRequest);
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#markAsReadRequest.playerId)")
    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(@RequestBody MarkAsReadRequest markAsReadRequest) {
        messagingService.markMessagesAsRead(markAsReadRequest);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/conversation")
    public ResponseEntity<FullConversationDTO> getFullConversation(@RequestBody ConversationRequest request) {
        return ResponseEntity.ok(messagingService.getFullConversation(request));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/conversation/{conversationId}/player/{playerId}")
    public ResponseEntity<ConversationPreviewDTO> getPreviewConversation(@PathVariable long conversationId, long playerId) {
        return ResponseEntity.ok(messagingService.getPreviewConversation(conversationId, playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/conversations/{playerId}")
    public ResponseEntity<List<ConversationPreviewDTO>> getPlayerConversations(@PathVariable long playerId) {
        return ResponseEntity.ok(messagingService.getPlayerPreviewConversations(playerId));
    }
}
