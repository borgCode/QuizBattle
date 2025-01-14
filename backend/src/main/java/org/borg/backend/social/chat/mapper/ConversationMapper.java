package org.borg.backend.social.chat.mapper;

import org.borg.backend.social.chat.dto.ConversationPreviewDTO;
import org.borg.backend.social.chat.dto.FullConversationDTO;
import org.borg.backend.social.chat.model.Conversation;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ConversationMapper {

    public static ConversationPreviewDTO toPreviewDTO(Conversation conversation, Long currentPlayerId) {
        if (conversation == null) {
            return null;
        }

        

        boolean isRead = isRead(conversation, currentPlayerId);

        return ConversationPreviewDTO.builder()
                .id(conversation.getId())
                .otherPlayer(PlayerMapper.toPlayerConversationDTO(otherPlayer))
                .latestMessageIsRead(isRead)
                .latestMessage(conversation.getLatestMessage().getContent())
                .build();
    }

    
    public static List<ConversationPreviewDTO> multipleToDTO(List<Conversation> conversations, Long currentPlayerId) {
        if (conversations == null || conversations.isEmpty()) {
            return List.of();
        }

        return conversations.stream()
                .map(conversation -> toPreviewDTO(conversation, currentPlayerId))
                .collect(Collectors.toList());
    }

    public static FullConversationDTO toFullConversationDTO(Conversation conversation, Long currentPlayerId) {
        if (conversation == null) {
            return null;
        }

        Player otherPlayer = conversation.getPlayer1().getId().equals(currentPlayerId)
                ? conversation.getPlayer2()
                : conversation.getPlayer1();
        
        boolean isRead = isRead(conversation, currentPlayerId);

        return FullConversationDTO.builder()
                .id(conversation.getId())
                .otherPlayer(PlayerMapper.toPlayerConversationDTO(otherPlayer))
                .messages(MessageMapper.multipleToDTO(conversation.getMessages()))
                .latestMessageIsRead(isRead)
                .build();
    }

    private static boolean isRead(Conversation conversation, Long currentPlayerId) {
        if (conversation.getLatestMessage() == null) {
            return true;
        }
        return conversation.getLatestMessage().getSenderId().equals(currentPlayerId) ||
                (conversation.getLatestMessage().getReceiverId().equals(currentPlayerId) &&
                        conversation.getLatestMessage().isRead());
    }
}
