package org.borg.backend.social.chat.mapper;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.social.chat.dto.ConversationPreviewDTO;
import org.borg.backend.social.chat.dto.FullConversationDTO;
import org.borg.backend.social.chat.model.Conversation;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, MessageMapper.class})
public abstract class ConversationMapper {

    @Autowired
    protected PlayerMapper playerMapper;
    @Autowired
    protected MessageMapper messageMapper;
    
    @Mapping(target = "otherPlayer", expression = "java(playerMapper.toPlayerConversationDTO(conversation.getOtherPlayer(currentPlayerId)))")
    @Mapping(target = "latestMessageIsRead", expression = "java(conversation.isRead(currentPlayerId))")
    @Mapping(target = "latestMessage", source = "latestMessage.content")
    public abstract ConversationPreviewDTO toPreviewDTO(Conversation conversation, @Context Long currentPlayerId);

    public abstract List<ConversationPreviewDTO> multipleToDTO(List<Conversation> conversations, @Context Long currentPlayerId);

    @Mapping(target = "otherPlayer", expression = "java(playerMapper.toPlayerConversationDTO(conversation.getOtherPlayer(currentPlayerId)))")
    @Mapping(target = "messages", expression = "java(messageMapper.multipleToDTO(conversation.getMessages()))")
    @Mapping(target = "latestMessageIsRead", expression = "java(conversation.isRead(currentPlayerId))")
    public abstract FullConversationDTO toFullConversationDTO(Conversation conversation, @Context Long currentPlayerId);
}
