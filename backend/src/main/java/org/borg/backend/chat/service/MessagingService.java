package org.borg.backend.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.chat.model.Message;
import org.borg.backend.chat.repository.ConversationRepository;
import org.borg.backend.chat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void sendMessage(SendMessageRequest request) {
        Conversation conversation;
        if (request.getConversationId() == null) {
            conversation = conversationRepository.save(Conversation.builder()
                    .player1Id(request.getSenderId())
                    .player2Id(request.getReceiverId())
                    .build());
        } else {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        }

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .senderId(request.getSenderId())
                .content(request.getMessage())
                .sentAt(Instant.now())
                .isRead(false)
                .build());

        conversation.setLatestMessage(message);
        conversationRepository.save(conversation);
    }
}
