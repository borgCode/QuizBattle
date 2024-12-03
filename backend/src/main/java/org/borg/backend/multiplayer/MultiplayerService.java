package org.borg.backend.multiplayer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiplayerService {
    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    
    public MatchmakingResponse findMatch(MatchmakingRequest request) {
        synchronized (matchmakingQueue) {
            Optional<Long> opponent = matchmakingQueue.stream().findFirst();
            
            if (opponent.isPresent()) {
                matchmakingQueue.remove(opponent.get());
                MultiplayerSession session = new MultiplayerSession(request.getPlayerId(), opponent.get());
                multiplayerSessionRepository.save(session);
                return new MatchmakingResponse(MatchStatus.MATCHED, session.getId());
            }
            
            matchmakingQueue.add(request.getPlayerId());
            return new MatchmakingResponse(MatchStatus.WAITING, null);
        }
    }

    public void cancelMatchmaking(Long playerId) {
        matchmakingQueue.remove(playerId);
    }

    public GameStateResponse getGameState(Long sessionId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        return new GameStateResponse(
                multiplayerSession.getPlayerIds(),
                multiplayerSession.getCurrentQuestionIndex(),
                multiplayerSession.getScore(),
                multiplayerSession.getStatus()
        );
    }

    public GameStateUpdate updateGameState(Long sessionId, Long playerId, boolean isCorrect) {
        
    }
}
