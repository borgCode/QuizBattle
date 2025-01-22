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
        log.info("Creating new conversation between sender: {} and receiver: {}", senderId, receiverId);
        Player player1 = playerService.getPlayerById(senderId);
        Player player2 = playerService.getPlayerById(receiverId);

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .player1(player1)
                .player2(player2)
                .build());

        log.debug("Successfully created conversation with id: {}", conversation.getId());
        return conversationMapper.toFullConversationDTO(conversation, senderId);
    }

    @Transactional
    public void sendMessage(SendMessageRequest request) {
        log.info("Processing message send request for conversation: {}, sender: {}, receiver: {}",
                request.getConversationId(), request.getSenderId(), request.getReceiverId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> {
                    log.error("Conversation not found with id: {}", request.getConversationId());
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Conversation not found for " + request.getConversationId());
                });

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getMessage())
                .read(false)
                .build());

        conversation.setLatestMessage(message);
        conversationRepository.save(conversation);
        log.debug("Saved message with id: {} in conversation: {}", message.getId(), conversation.getId());

        try {
            simpMessagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/message", messageMapper.toDTO(message)
            );
            log.debug("Successfully sent message notification to user: {}", request.getReceiverUsername());
        } catch (Exception e) {
            log.error("Failed to send message notification to user: {}", request.getReceiverUsername(), e);
        }

        boolean isLastMessageRead = conversationRepository.isLatestMessageRead(conversation.getId());

        if (!isLastMessageRead) {
            try {
                simpMessagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/conversation/unread", conversation.getId());
                log.debug("Sent unread conversation notification to user: {}", request.getReceiverUsername());
            } catch (Exception e) {
                log.error("Failed to send unread conversation notification to user: {}",
                        request.getReceiverUsername(), e);
            }
        }
    }

    public void markMessagesAsRead(MarkAsReadRequest markAsReadRequest) {
        List<Long> messageIds = markAsReadRequest.getMessageIds();
        Long playerId = markAsReadRequest.getPlayerId();

        log.info("Marking messages as read for player: {}, message count: {}",
                playerId, messageIds.size());

        if (messageIds.isEmpty()) {
            log.debug("No messages to mark as read");
            return;
        }
        long invalidIdCount = messageRepository.countByIdInAndReceiverIdNot(messageIds, playerId);

        if (invalidIdCount > 0) {
            log.warn("Unauthorized attempt to mark messages as read. Player: {}, Invalid message count: {}",
                    playerId, invalidIdCount);
            throw new AccessDeniedException("Not authorized to mark messages as read");
        }

        messageRepository.markMessagesAsRead(messageIds);
        log.debug("Successfully marked {} messages as read", messageIds.size());

        Long conversationId = markAsReadRequest.getConversationId();

        boolean isLastMessageRead = conversationRepository.isLatestMessageRead(conversationId);

        if (isLastMessageRead) {
            log.debug("Latest message is read for conversation: {}, sending notification to user: {}",
                    conversationId, markAsReadRequest.getUsername());
            try {
                simpMessagingTemplate.convertAndSendToUser(markAsReadRequest.getUsername(), "/queue/conversation/read", conversationId);
            } catch (Exception e) {
                log.error("Failed to send read conversation notification to user: {}",
                        markAsReadRequest.getUsername(), e);
            }
        }
    }

    public FullConversationDTO getFullConversation(ConversationRequest conversationRequest) {
        log.info("Retrieving full conversation for sender: {} and receiver: {}",
                conversationRequest.getSenderId(), conversationRequest.getReceiverId());

        Conversation conversation = conversationRepository.findByBothPlayerIds(conversationRequest.getSenderId(), conversationRequest.getReceiverId());

        if (conversation == null) {
            log.debug("No existing conversation found, creating new conversation");
            return createConversation(conversationRequest.getSenderId(), conversationRequest.getReceiverId());
        }
        log.debug("Found existing conversation with id: {}", conversation.getId());
        return conversationMapper.toFullConversationDTO(conversation, conversationRequest.getSenderId());
    }

    public ConversationPreviewDTO getPreviewConversation(long conversationId, long playerId) {
        log.info("Retrieving conversation preview for conversation: {} and player: {}", conversationId, playerId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    log.error("Conversation not found with id: {}", conversationId);
                    return new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND,
                            String.format("Conversation not found for id: %s", conversationId));
                });

        if (conversation.getPlayer1().getId().equals(playerId) || conversation.getPlayer2().getId().equals(playerId)) {
            log.debug("Player {} authorized to view conversation {}", playerId, conversationId);
            return conversationMapper.toPreviewDTO(conversation, playerId);
        } else {
            log.warn("Unauthorized attempt to view conversation {}. Player: {}", conversationId, playerId);
            throw new AccessDeniedException("Not authorized to view this conversation");
        }
    }

    public List<ConversationPreviewDTO> getPlayerPreviewConversations(Long playerId) {
        log.info("Retrieving all conversation previews for player: {}", playerId);
        return conversationMapper.multipleToDTO(conversationRepository.findConversationsByPlayerId(playerId), playerId);
    }
}
