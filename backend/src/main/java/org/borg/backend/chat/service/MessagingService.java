package org.borg.backend.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.chat.dto.ConversationPreviewDTO;
import org.borg.backend.chat.dto.ConversationRequest;
import org.borg.backend.chat.dto.FullConversationDTO;
import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.mapper.ConversationMapper;
import org.borg.backend.chat.mapper.MessageMapper;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.chat.model.Message;
import org.borg.backend.chat.repository.ConversationRepository;
import org.borg.backend.chat.repository.MessageRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Transactional
    public void sendMessage(SendMessageRequest request) {

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getMessage())
                .sentAt(Instant.now())
                .isRead(false)
                .build());

        conversation.setLatestMessage(message);
        conversationRepository.save(conversation);

        simpMessagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/message", MessageMapper.toDTO(message));

    }

    public void markMessagesAsRead(List<Long> messageIds, long playerId) {
        if (messageIds.isEmpty()) {
            return;
        }
        long invalidIdCount = messageRepository.countByIdInAndReceiverIdNot(messageIds, playerId);
        
        if (invalidIdCount > 0) {
            throw new AccessDeniedException("Not authorized to mark messages as read");
        }


        messageRepository.markMessagesAsRead(messageIds);
    }


    public FullConversationDTO getConversation(ConversationRequest conversationRequest) {
        Conversation conversation = conversationRepository.findByBothPlayerIds(conversationRequest.getSenderId(), conversationRequest.getReceiverId());
        if (conversation == null) {
            return createConversation(conversationRequest.getSenderId(), conversationRequest.getReceiverId());
        }
        
        
        return ConversationMapper.toFullConversationDTO(conversation, conversationRequest.getSenderId());
    }

    public FullConversationDTO createConversation(Long senderId, Long receiverId) {
        Player player1 = playerRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));
        Player player2 = playerRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found"));

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .player1(player1)
                .player2(player2)
                .build());
        return ConversationMapper.toFullConversationDTO(conversation, senderId);
    }

    public List<ConversationPreviewDTO> getPlayerConversations(Long playerId) {
        return ConversationMapper.multipleToDTO(conversationRepository.findConversationsByPlayerId(playerId), playerId);
    }
    
}
