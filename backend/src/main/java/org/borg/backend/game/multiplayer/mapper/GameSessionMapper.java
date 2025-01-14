package org.borg.backend.game.multiplayer.mapper;

import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.player.mapper.PlayerMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = PlayerMapper.class)
public abstract class GameSessionMapper {
    
    @Autowired
    protected PlayerMapper playerMapper;

    @Mapping(target = "playerTurn", source = "currentPlayerTurn.id")
    @Mapping(target = "playerDTOS", expression = "java(playerMapper.multipleToDTO(multiplayerSession.getPlayers()))")
    @Mapping(target = "playerWhoGaveUp", expression = "java(multiplayerSession.getPlayerWhoGaveUpId())")
    public abstract GameStateResponse toGameStateResponse(MultiplayerSession multiplayerSession);

    @Mapping(target = "playerDTOS", expression = "java(playerMapper.multipleToDTO(multiplayerSession.getPlayers()))")
    public abstract MultiplayerSessionDTO toMultiplayerSessionDTO(MultiplayerSession multiplayerSession);
    public abstract List<MultiplayerSessionDTO> multipleToMultiplayerSessionDTO(List<MultiplayerSession> multiplayerSessions);
}
