package org.borg.backend.social.chat.mapper;

import org.borg.backend.social.chat.dto.MessageDTO;
import org.borg.backend.social.chat.model.Message;

import java.util.List;

public class MessageMapper {

    public static MessageDTO toDTO(Message message) {
        if (message == null) {
            return null;
        }

        return MessageDTO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .content(message.getContent())
                .build();
    }

    public static List<MessageDTO> multipleToDTO(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .map(MessageMapper::toDTO)
                .toList();
    }
}
