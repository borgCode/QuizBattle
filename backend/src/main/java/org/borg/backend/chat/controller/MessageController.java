package org.borg.backend.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
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
    public void sendMessage(@Payload SendMessageRequest messageRequest) {
        log.warn("Receiving message");
        messagingService.sendMessage(messageRequest);
    }
    
    @PostMapping("/conversation")
    public ResponseEntity<FullConversationDTO> getConversation(@RequestBody ConversationRequest request) {
        return ResponseEntity.ok(messagingService.getConversation(request));
    }
    
    @GetMapping("/conversations/{playerId}")
    public ResponseEntity<List<ConversationPreviewDTO>> getPlayerConversations(@PathVariable Long playerId) {
        return ResponseEntity.ok(messagingService.getPlayerConversations(playerId));
    }
}
