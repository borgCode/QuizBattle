package org.borg.backend.multiplayer;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.Player;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.player.PlayerRepository;
import org.borg.backend.question.Question;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiplayerService {
    private final List<Long> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final PlayerRepository playerRepository;
    

    public MatchmakingResponse findMatch(Long playerId) {
        synchronized (matchmakingQueue) {
            Optional<Long> opponentId = matchmakingQueue.stream().findFirst();

            if (opponentId.isPresent()) {
                matchmakingQueue.remove(opponentId.get());

                Player requestingPlayer = playerRepository.findById(playerId)
                        .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
                Player opponent = playerRepository.findById(opponentId.get())
                        .orElseThrow(() -> new NoSuchElementException("Opponent not found"));

                //Randomly choose who starts
                
                Player startingPlayer = Math.random() < 0.5 ? requestingPlayer : opponent;

                MultiplayerSession session = new MultiplayerSession(requestingPlayer, opponent, startingPlayer);
                multiplayerSessionRepository.save(session);
                return new MatchmakingResponse(MatchStatus.MATCHED, session.getId());
            }

            matchmakingQueue.add(playerId);
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
                multiplayerSession.getCurrentPlayerTurn().getId(),
                PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()),
                multiplayerSession.getCurrentQuestionIndex(),
                multiplayerSession.getScore(),
                multiplayerSession.getStatus(),
                multiplayerSession.getQuestionResults()
        );
    }

    public synchronized void updateGameState(Long sessionId, Long playerId, boolean isCorrect) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        //Increment score if answer was correct
        if (isCorrect) {
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
        }
        
        //Update questions answered
        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);

        //Find opponent in session
        Player opponent = session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));
        
        if(isGameComplete(questionsAnswered)) {
            session.setStatus(GameStatus.COMPLETED);
        } else {
            //Update question index to manage turns
            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);

            //Determine if it's the opponent's turn
            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    session.setCurrentPlayerTurn(opponent);
                }
            }
        }
        
        multiplayerSessionRepository.save(session);
        
    }

    private boolean isGameComplete(Map<Long, Integer> questionsAnswered) {
        return questionsAnswered.values().stream()
                .allMatch(count -> count == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER);
    }

    public void updateSessionQuestionsAndCategory(Long sessionId, List<Question> questions, String selectedCategory) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
        }
        session.getPlayedCategories().add(selectedCategory);
        multiplayerSessionRepository.save(session);
    }

    public List<MultiplayerSessionDTO> getMultiplayerSessionsById(Long playerId) {
        List<MultiplayerSession> multiplayerSessions = multiplayerSessionRepository.findByPlayerId(playerId);
        List<MultiplayerSessionDTO> multiplayerSessionDTOS = new ArrayList<>();
        for (MultiplayerSession multiplayerSession : multiplayerSessions) {
            MultiplayerSessionDTO multiplayerSessionDTO = MultiplayerSessionDTO.builder()
                    .id(multiplayerSession.getId())
                    .playerDTOList(PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()))
                    .score(multiplayerSession.getScore())
                    .status(multiplayerSession.getStatus())
                    .currentPlayerTurn(PlayerMapper.toDTO(multiplayerSession.getCurrentPlayerTurn()))
                    .build();
            multiplayerSessionDTOS.add(multiplayerSessionDTO);
        }
        return multiplayerSessionDTOS;
    }
}
            
