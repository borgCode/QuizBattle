package org.borg.backend.chat.mapper;

import org.borg.backend.chat.dto.ConversationPreviewDTO;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationMapper {

    public static ConversationPreviewDTO toDTO(Conversation conversation, Long currentPlayerId) {
        if (conversation == null) {
            return null;
        }

        Player otherPlayer = conversation.getPlayer1().getId().equals(currentPlayerId)
                ? conversation.getPlayer2()
                : conversation.getPlayer1();

        return ConversationPreviewDTO.builder()
                .id(conversation.getId())
                .otherPlayer(PlayerMapper.toPlayerConversationDTO(otherPlayer))
                .latestMessageIsRead(conversation.isLatestMessageIsRead())
                .latestMessage(conversation.getLatestMessage().getContent())
                .build();
    }

    public static List<ConversationPreviewDTO> multipleToDTO(List<Conversation> conversations, Long currentPlayerId) {
        if (conversations == null || conversations.isEmpty()) {
            return Collections.emptyList();
        }

        return conversations.stream()
                .map(conversation -> toDTO(conversation, currentPlayerId))
                .collect(Collectors.toList());
    }
}
