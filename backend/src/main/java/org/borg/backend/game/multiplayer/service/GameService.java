package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.mapper.GameSessionMapper;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.social.notification.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.borg.backend.game.multiplayer.util.MultiplayerGameConstants.QUESTIONS_PER_ROUND;
import static org.borg.backend.game.multiplayer.util.MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RoundSessionService roundSessionService;
    private final StatsService statsService;
    private final MultiplayerSessionService multiplayerSessionService;
    private final GameSessionMapper gameSessionMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public GameStateResponse getGameState(long sessionId, long playerId) {
        log.debug("Fetching game state for sessionId: {}, playerId: {}", sessionId, playerId);
        MultiplayerSession multiplayerSession = multiplayerSessionService.getSessionById(sessionId);

        SessionPlayer sessionPlayer = multiplayerSession.getSessionPlayerByPlayerId(playerId);
        if (sessionPlayer == null) {
            log.warn("Access denied for playerId: {} in sessionId: {}", playerId, sessionId);
            throw new AccessDeniedException("Not authorized to access this game session");
        }
        
        log.debug("Game state fetched successfully for sessionId: {}, playerId: {}", sessionId, playerId);
        return gameSessionMapper.toGameStateResponse(multiplayerSession, playerId);
    }

    @Transactional
    public void updateGameState(Long sessionId, Long playerId, Long questionId, boolean isCorrect) {
        log.debug("Updating game state for sessionId: {}, playerId: {}, questionId: {}, isCorrect: {}",
                sessionId, playerId, questionId, isCorrect);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);

        SessionPlayer sessionPlayer = session.getSessionPlayerByPlayerId(playerId);
        SessionPlayer opponent = session.getOpponentSessionPlayerId(playerId);
        
        if (opponent == null) {
            throw new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session");
        }
        
        if (isCorrect) {
            log.debug("Updating score for playerId: {}", playerId);
            sessionPlayer.setScore(sessionPlayer.getScore() + 1);
            log.debug("PlayerId: {} new score: {}", playerId, sessionPlayer.getScore());
        }
        
        sessionPlayer.setQuestionsAnswered(sessionPlayer.getQuestionsAnswered() + 1);
        log.debug("PlayerId: {} questions answered updated to: {}", playerId, sessionPlayer.getQuestionsAnswered());

        sessionPlayer.getQuestionResults().add(new PlayerQuestionResult(questionId, sessionPlayer.getQuestionsAnswered() - 1, isCorrect));
        
        boolean isGameComplete = isGameComplete(sessionPlayer, opponent);
        if (isGameComplete) {
            log.debug("Game is complete for sessionId: {}", sessionId);
            
            session.setStatus(GameStatus.COMPLETED);
            determineGameOutcome(session);
            sendGameOverNotifications(session);
        } else {

            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % QUESTIONS_PER_ROUND);

            if (session.getCurrentQuestionIndex() == 0) {
                if (sessionPlayer.getQuestionsAnswered() > opponent.getQuestionsAnswered()) {
                    session.setCurrentPlayerTurnId(opponent.getPlayer().getId());
                    log.debug("Turn changed to opponent: {} for sessionId: {}", opponent.getPlayer().getId(), sessionId);
                    simpMessagingTemplate.convertAndSendToUser(opponent.getPlayer().getUsername(), "queue/session/" + sessionId, "refresh");
                } else {
                    session.getQuestionIds().clear();
                    roundSessionService.finishSession(playerId);
                }
            }
        }
        multiplayerSessionRepository.save(session);
        log.debug("Game state updated successfully for sessionId: {}", sessionId);
        
        if (isGameComplete) {
            simpMessagingTemplate.convertAndSendToUser(opponent.getPlayer().getUsername(), "queue/session/" + sessionId, "refresh");
        }
    }

    private boolean isGameComplete(SessionPlayer player, SessionPlayer opponent) {
        return player.getQuestionsAnswered() == TOTAL_QUESTIONS_PER_PLAYER && opponent.getQuestionsAnswered() == TOTAL_QUESTIONS_PER_PLAYER;
    }

    private void determineGameOutcome(MultiplayerSession session) {
        log.debug("Determining game outcome for sessionId: {}", session.getId());
        List<SessionPlayer> sessionPlayers = session.getSessionPlayers();

        Long player1Id = sessionPlayers.get(0).getPlayer().getId();
        Long player2Id = sessionPlayers.get(1).getPlayer().getId();
        
        int score1 = sessionPlayers.get(0).getScore();
        int score2 = sessionPlayers.get(1).getScore();

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

        log.info("Game outcome determined for sessionId: {}, result: {}", session.getId(), result);
        statsService.updateGameStats(sessionPlayers.get(0).getPlayer(), sessionPlayers.get(1).getPlayer(), result);
        
        if (!result.equals(GameResult.TIE)) {
            applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
        }
        log.debug("GameWonEvent published for sessionId: {}", session.getId());
    }

    private void sendGameOverNotifications(MultiplayerSession session) {
        log.debug("Sending game over notifications for sessionId: {}", session.getId());
        if (session.getIsTie()) {
            notificationService.sendTieNotifications(session.getSessionPlayers(), session.getId());
        } else {
            SessionPlayer loserSessionPlayer = session.getSessionPlayerByPlayerId(session.getLoserId());
            SessionPlayer winnerSessionPlayer = session.getSessionPlayerByPlayerId(session.getWinnerId());
            
            notificationService.sendGameWonNotification(
                    winnerSessionPlayer.getPlayer().getId(),
                    loserSessionPlayer.getPlayer().getDisplayName(),
                    session.getId()
            );
            notificationService.sendGameLostNotification(
                    loserSessionPlayer.getPlayer().getId(),
                    winnerSessionPlayer.getPlayer().getDisplayName(),
                    session.getId()
            );
        }
    }
    
    public void updateSessionQuestionsAndCategory(MultiplayerSession session, List<Long> questionsIds, String selectedCategory) {
        log.info("Updating session questions and category for sessionId: {}", session.getId());
        
        session.getQuestionIds().clear();
        
        session.setQuestionIds(new ArrayList<>(questionsIds));
        log.debug("Added new session questions: {} for session {}", questionsIds, session);
        
        session.getPlayedCategories().add(selectedCategory);
        log.debug("Added category: {} to played session categories", selectedCategory);

        session.getRoundCategories().add(selectedCategory);
        log.debug("Added to round categories: {}", selectedCategory);
        multiplayerSessionRepository.save(session);
        log.debug("Saved session {}", session.getId());
    }
    
    public void acknowledgeGameOver(Long sessionId, Long playerId) {
        log.info("Acknowledging game over for playerId: {} in sessionId: {}", playerId, sessionId);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);
        
        session.getSessionPlayerByPlayerId(playerId).setHasAcknowledgedGameOver(true);
        multiplayerSessionRepository.save(session);
        log.debug("Marked acknowledgement as true for playerId: {} and saved", playerId);
    }

    @Transactional
    public void handleGiveUp(Long sessionId, Long playerId) {
        log.info("Handling give-up for sessionId: {} and playerId: {}", sessionId, playerId);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);

        SessionPlayer sessionPlayer = session.getSessionPlayerByPlayerId(playerId);

        sessionPlayer.setGivenUp(true);
        session.setStatus(GameStatus.COMPLETED);
        log.debug("Session status set to COMPLETED for sessionId: {}", sessionId);

        session.setLoserId(sessionPlayer.getPlayer().getId());
        log.debug("Player with ID {} marked as loser for sessionId: {}", playerId, sessionId);


        SessionPlayer opponent = session.getSessionPlayers().stream()
                .filter(sp -> !sp.getId().equals(sessionPlayer.getId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Opponent not found for sessionId: {}", sessionId);
                    return new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session");
                });

        session.setWinnerId(opponent.getPlayer().getId());

        multiplayerSessionRepository.save(session);
        
        Player loserPlayer = sessionPlayer.getPlayer();
        Player winnerPlayer = opponent.getPlayer();
        
        statsService.updateGameStats(loserPlayer, winnerPlayer, GameResult.WIN_PLAYER2);
        
        notificationService.sendGameWonNotification(winnerPlayer.getId(), loserPlayer.getDisplayName(), sessionId);
        notificationService.sendGameLostNotification(loserPlayer.getId(), winnerPlayer.getDisplayName(), sessionId);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(winnerPlayer.getId()));
    }
}
