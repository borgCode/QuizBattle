package org.borg.backend.chat.mapper;

import org.borg.backend.chat.dto.MessageDTO;
import org.borg.backend.chat.model.Message;

import java.util.Collections;
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
                .isRead(message.isRead())
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
