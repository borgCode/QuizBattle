package org.borg.backend.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.borg.backend.chat.dto.ConversationPreviewDTO;
import org.borg.backend.chat.dto.FullConversationDTO;
import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.mapper.ConversationMapper;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.chat.model.Message;
import org.borg.backend.chat.repository.ConversationRepository;
import org.borg.backend.chat.repository.MessageRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public void sendMessage(SendMessageRequest request) {
        Conversation conversation;
        if (request.getConversationId() == null) {
            Player player1 = playerRepository.findById(request.getSenderId())
                    .orElseThrow(() -> new EntityNotFoundException("Sender not found"));
            Player player2 = playerRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new EntityNotFoundException("Receiver not found"));

            conversation = conversationRepository.save(Conversation.builder()
                    .player1(player1)
                    .player2(player2)
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
    public FullConversationDTO getConversation(Long conversationId) {
        //TODO implement
        return null;
    }
    
    public List<ConversationPreviewDTO> getPlayerConversations(Long playerId) {
        return ConversationMapper.multipleToDTO(conversationRepository.findConversationsByPlayerId(playerId), playerId);
    }

    
}
