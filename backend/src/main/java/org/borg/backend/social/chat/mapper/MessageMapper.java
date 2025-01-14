package org.borg.backend.social.chat.mapper;

import org.borg.backend.social.chat.dto.MessageDTO;
import org.borg.backend.social.chat.model.Message;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageDTO toDTO(Message message);
    List<MessageDTO> multipleToDTO(List<Message> messages);
}
