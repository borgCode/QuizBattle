package org.borg.backend.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.chat.dto.ConversationPreviewDTO;
import org.borg.backend.chat.dto.ConversationRequest;
import org.borg.backend.chat.dto.FullConversationDTO;
import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.service.MessagingService;
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

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(@RequestParam List<Long> messageIds, @RequestParam long playerId) {
        messagingService.markMessagesAsRead(messageIds, playerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.senderId)")
    @PostMapping("/conversation")
    public ResponseEntity<FullConversationDTO> getConversation(@RequestBody ConversationRequest request) {
        return ResponseEntity.ok(messagingService.getConversation(request));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/conversations/{playerId}")
    public ResponseEntity<List<ConversationPreviewDTO>> getPlayerConversations(@PathVariable long playerId) {
        return ResponseEntity.ok(messagingService.getPlayerConversations(playerId));
    }
}
