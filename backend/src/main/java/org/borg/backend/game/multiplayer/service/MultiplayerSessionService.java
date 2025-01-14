package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.mapper.GameSessionMapper;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MultiplayerSessionService {

    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final GameSessionMapper gameSessionMapper;

    public MultiplayerSession getSessionById(Long sessionId) {
        return multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Session not found for " + sessionId));
    }

    public List<MultiplayerSessionDTO> getMultiplayerSessionsById(Long playerId) {
        return gameSessionMapper.multipleToMultiplayerSessionDTO(multiplayerSessionRepository.findByPlayerId(playerId));
    }
}
