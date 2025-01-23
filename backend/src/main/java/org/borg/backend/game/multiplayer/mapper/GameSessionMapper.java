package org.borg.backend.game.multiplayer.mapper;

import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = SessionPlayerMapper.class)
public abstract class GameSessionMapper {
    
    @Autowired
    protected SessionPlayerMapper sessionPlayerMapper;
    
    
    @Mapping(target = "playerTurn", source = "multiplayerSession.currentPlayerTurnId")
    @Mapping(target = "playerDTO", expression = "java(sessionPlayerMapper.toDTO(multiplayerSession.getSessionPlayerByPlayerId(playerId)))")
    @Mapping(target = "opponentDTO", expression = "java(sessionPlayerMapper.toDTO(multiplayerSession.getOpponentSessionPlayerId(playerId)))")
    @Mapping(target = "playerWhoGaveUp", expression = "java(multiplayerSession.getPlayerWhoGaveUpId())")
    public abstract GameStateResponse toGameStateResponse(MultiplayerSession multiplayerSession, Long playerId);

    @Mapping(target = "playerDTO", expression = "java(sessionPlayerMapper.toDTO(multiplayerSession.getSessionPlayerByPlayerId(playerId)))")
    @Mapping(target = "opponentDTO", expression = "java(sessionPlayerMapper.toDTO(multiplayerSession.getOpponentSessionPlayerId(playerId)))")
    @Mapping(target = "currentPlayerTurn", source = "multiplayerSession.currentPlayerTurnId")
    public abstract MultiplayerSessionDTO toMultiplayerSessionDTO(MultiplayerSession multiplayerSession, Long playerId);
    
    public Page<MultiplayerSessionDTO> multipleToMultiplayerSessionDTO(Page<MultiplayerSession> multiplayerSessions, Long playerId) {
        return  multiplayerSessions.map(session -> toMultiplayerSessionDTO(session, playerId));
    }
}
