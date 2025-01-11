package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.events.StatsEvents;
import org.borg.backend.shared.enums.GameResult;
import org.borg.backend.shared.enums.GameStatus;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.PlayerQuestionResult;
import org.borg.backend.question.model.Question;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.Map.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {


    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final PlayerRepository playerRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RoundSessionService roundSessionService;

    public GameStateResponse getGameState(long sessionId, long playerId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        boolean playerExistsInSession = multiplayerSession.getPlayers().stream()
                .anyMatch(player -> player.getId().equals(playerId));
        
        if (!playerExistsInSession) {
            throw new AccessDeniedException("Not authorized to access this game session");
        }

        Long playerWhoGaveUp = multiplayerSession.getPlayerHasGivenUp().entrySet().stream()
                .filter(Entry::getValue)
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);
        
        return GameStateResponse.builder()
                .playerTurn(multiplayerSession.getCurrentPlayerTurn().getId())
                .playerDTOS(PlayerMapper.multipleToDTO(multiplayerSession.getPlayers()))
                .currentQuestionIndex(multiplayerSession.getCurrentQuestionIndex())
                .scores(multiplayerSession.getScore())
                .status(multiplayerSession.getStatus())
                .results(multiplayerSession.getQuestionResults())
                .questionIds(multiplayerSession.getQuestionIds())
                .roundCategories(multiplayerSession.getRoundCategories())
                .playerAcknowledgment(multiplayerSession.getPlayerAcknowledgment())
                .playerWhoGaveUp(playerWhoGaveUp)
                .winnerId(multiplayerSession.getWinnerId())
                .loserId(multiplayerSession.getLoserId())
                .isTie(multiplayerSession.getIsTie())
                .build();

    }

    @Transactional
    public synchronized void updateGameState(Long sessionId, Long playerId, Long questionId, boolean isCorrect) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        if (isCorrect) {
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
        }

        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);

        session.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, session.getQuestionsAnswered().get(playerId) - 1, isCorrect));

        Player opponent = session.getPlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));

        if (isGameComplete(questionsAnswered)) {
            session.setStatus(GameStatus.COMPLETED);
            determineGameOutcome(session);
            sendGameOverNotifications(session);

        } else {
            
            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);
            
            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    session.setCurrentPlayerTurn(opponent);
                } else {
                    session.getQuestionIds().clear();
                    roundSessionService.finishSession(playerId);
                }
            }
        }
        multiplayerSessionRepository.save(session);
    }

    private boolean isGameComplete(Map<Long, Integer> questionsAnswered) {
        return questionsAnswered.values().stream()
                .allMatch(count -> count == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER);
    }

    private void determineGameOutcome(MultiplayerSession session) {
        List<Player> players = session.getPlayers();
        
        Long player1Id = players.get(0).getId();
        Long player2Id = players.get(1).getId();

        Map<Long, Integer> scores = session.getScore();
        int score1 = scores.get(player1Id);
        int score2 = scores.get(player2Id);
        
        GameResult result;
        if (score1 > score2) {
            session.setWinnerId(player1Id);
            session.setLoserId(player2Id);
            session.setIsTie(false);
            result = GameResult.WIN_PLAYER1;
        } else if (score1 < score2) {
            session.setWinnerId(player2Id);
            session.setLoserId(player1Id);
            session.setIsTie(false);
            result = GameResult.WIN_PLAYER2;
        } else {
            session.setIsTie(true);
            result = GameResult.TIE;
        }
        
        applicationEventPublisher.publishEvent(new StatsEvents.GameCompletedEvent(players.get(0), players.get(1), result));
    }

    private void sendGameOverNotifications(MultiplayerSession session) {
        if (session.getIsTie()) {
            notificationService.sendTieNotifications(session.getPlayers(), session.getId());
        } else {
            Player loserPlayer = findPlayerInSession(session, session.getLoserId());
            Player winnerPlayer = findPlayerInSession(session, session.getWinnerId());
            notificationService.sendGameWonNotification(session.getWinnerId(), loserPlayer.getDisplayName(), session.getId());
            notificationService.sendGameLostNotification(session.getLoserId(), winnerPlayer.getDisplayName(), session.getId());
        }
    }

    private Player findPlayerInSession(MultiplayerSession session, Long playerId) {
        return session.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found"));
    }

    public void updateSessionQuestionsAndCategory(MultiplayerSession session, List<Question> questions, String selectedCategory) {
        log.warn("Session ids: " + session.getQuestionIds());
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
        }
        session.getPlayedCategories().add(selectedCategory);

        session.getRoundCategories().add(selectedCategory);
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
    
    public void acknowledgeGameOver(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        session.getPlayerAcknowledgment().put(playerId, true);
        multiplayerSessionRepository.save(session);
    }

    @Transactional
    public void handleGiveUp(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        session.getPlayerHasGivenUp().put(playerId, true);
        session.setStatus(GameStatus.COMPLETED);

        session.setLoserId(playerId);
        session.setWinnerId(session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .map(Player::getId)
                .orElseThrow(() -> new IllegalStateException("Player not found in session")));

        multiplayerSessionRepository.save(session);

        Player loserPlayer = findPlayerInSession(session, session.getLoserId());
        Player winnerPlayer = findPlayerInSession(session, session.getWinnerId());

        loserPlayer.getStats().incrementLosses();
        winnerPlayer.getStats().incrementWins();

        playerRepository.saveAll(List.of(loserPlayer, winnerPlayer));

        notificationService.sendGameWonNotification(winnerPlayer.getId(), loserPlayer.getDisplayName(), sessionId);
        notificationService.sendGameLostNotification(loserPlayer.getId(), winnerPlayer.getDisplayName(), sessionId);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
    }
}
