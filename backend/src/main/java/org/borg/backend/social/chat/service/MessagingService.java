package org.borg.backend.social.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.social.chat.dto.*;
import org.borg.backend.social.chat.mapper.ConversationMapper;
import org.borg.backend.social.chat.mapper.MessageMapper;
import org.borg.backend.social.chat.model.Conversation;
import org.borg.backend.social.chat.model.Message;
import org.borg.backend.social.chat.repository.ConversationRepository;
import org.borg.backend.social.chat.repository.MessageRepository;
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
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PlayerService playerService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public FullConversationDTO createConversation(Long senderId, Long receiverId) {
        Player player1 = playerService.getPlayerById(senderId);
        Player player2 = playerService.getPlayerById(receiverId);

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .player1(player1)
                .player2(player2)
                .build());
        return conversationMapper.toFullConversationDTO(conversation, senderId);
    }

    @Transactional
    public void sendMessage(SendMessageRequest request) {

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Conversation not found for " + request.getConversationId()));

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getMessage())
                .sentAt(Instant.now())
                .read(false)
                .build());

        conversation.setLatestMessage(message);
        conversationRepository.save(conversation);

        simpMessagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/message", messageMapper.toDTO(message));

        boolean isLastMessageRead = conversationRepository.isLatestMessageRead(conversation.getId());

        if (!isLastMessageRead) {
            simpMessagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/conversation/unread", conversation.getId());
        }
    }

    public void markMessagesAsRead(MarkAsReadRequest markAsReadRequest) {
        List<Long> messageIds = markAsReadRequest.getMessageIds();
        Long playerId = markAsReadRequest.getPlayerId();

        if (messageIds.isEmpty()) {
            return;
        }
        long invalidIdCount = messageRepository.countByIdInAndReceiverIdNot(messageIds, playerId);

        if (invalidIdCount > 0) {
            throw new AccessDeniedException("Not authorized to mark messages as read");
        }

        messageRepository.markMessagesAsRead(messageIds);

        Long conversationId = markAsReadRequest.getConversationId();

        boolean isLastMessageRead = conversationRepository.isLatestMessageRead(conversationId);

        if (isLastMessageRead) {
            log.warn("Latest message is read for: {}", playerId);
            simpMessagingTemplate.convertAndSendToUser(markAsReadRequest.getUsername(), "/queue/conversation/read", conversationId);
        }
    }

    public FullConversationDTO getFullConversation(ConversationRequest conversationRequest) {
        Conversation conversation = conversationRepository.findByBothPlayerIds(conversationRequest.getSenderId(), conversationRequest.getReceiverId());
        if (conversation == null) {
            return createConversation(conversationRequest.getSenderId(), conversationRequest.getReceiverId());
        }

        return conversationMapper.toFullConversationDTO(conversation, conversationRequest.getSenderId());
    }

    public ConversationPreviewDTO getPreviewConversation(long conversationId, long playerId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                                String.format("Conversation not found for id: %s", conversationId)));
        
        if (conversation.getPlayer1().getId().equals(playerId) || conversation.getPlayer2().getId().equals(playerId)) {
            return conversationMapper.toPreviewDTO(conversation, playerId);
        } else {
            throw new AccessDeniedException("Not authorized to view this conversation");
        }
    }

    public List<ConversationPreviewDTO> getPlayerPreviewConversations(Long playerId) {
        return conversationMapper.multipleToDTO(conversationRepository.findConversationsByPlayerId(playerId), playerId);
    }
}
