package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.mapper.GameSessionMapper;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.social.notification.service.NotificationService;
import org.hibernate.Session;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.borg.backend.game.multiplayer.util.MultiplayerGameConstants.*;

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

    public GameStateResponse getGameState(long sessionId, long playerId) {
        log.debug("Fetching game state for sessionId: {}, playerId: {}", sessionId, playerId);
        MultiplayerSession multiplayerSession = multiplayerSessionService.getSessionById(sessionId);

        SessionPlayer sessionPlayer = multiplayerSession.getSessionPlayerById(playerId);
        if (sessionPlayer == null) {
            log.warn("Access denied for playerId: {} in sessionId: {}", playerId, sessionId);
            throw new AccessDeniedException("Not authorized to access this game session");
        }
        
        log.debug("Game state fetched successfully for sessionId: {}, playerId: {}", sessionId, playerId);
        return gameSessionMapper.toGameStateResponse(multiplayerSession, playerId);
    }

    @Transactional
    public synchronized void updateGameState(Long sessionId, Long playerId, Long questionId, boolean isCorrect) {
        log.debug("Updating game state for sessionId: {}, playerId: {}, questionId: {}, isCorrect: {}",
                sessionId, playerId, questionId, isCorrect);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);

        SessionPlayer sessionPlayer = session.getSessionPlayerById(playerId);
        if (isCorrect) {
            log.debug("Updating score for playerId: {}", playerId);
            sessionPlayer.setScore(sessionPlayer.getScore() + 1);
            log.debug("PlayerId: {} new score: {}", playerId, sessionPlayer.getScore());
        }
        
        sessionPlayer.setQuestionsAnswered(sessionPlayer.getQuestionsAnswered() + 1);
        log.debug("PlayerId: {} questions answered updated to: {}", playerId, sessionPlayer.getQuestionsAnswered());

        sessionPlayer.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, sessionPlayer.getQuestionsAnswered() - 1, isCorrect));

        SessionPlayer opponent = session.getSessionPlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session"));

        if (isGameComplete(sessionPlayer, opponent)) {
            log.debug("Game is complete for sessionId: {}", sessionId);
            session.setStatus(GameStatus.COMPLETED);
            determineGameOutcome(session);
            sendGameOverNotifications(session);
        } else {

            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % QUESTIONS_PER_ROUND);

            if (session.getCurrentQuestionIndex() == 0) {
                if (sessionPlayer.getQuestionsAnswered() > opponent.getQuestionsAnswered()) {
                    session.setCurrentPlayerTurnId(opponent.getId());
                    log.debug("Turn changed to opponent: {} for sessionId: {}", opponent.getId(), sessionId);
                } else {
                    session.getQuestionIds().clear();
                    roundSessionService.finishSession(playerId);
                }
            }
        }
        multiplayerSessionRepository.save(session);
        log.debug("Game state updated successfully for sessionId: {}", sessionId);
    }

    private boolean isGameComplete(SessionPlayer player, SessionPlayer opponent) {
        return player.getQuestionsAnswered() == TOTAL_QUESTIONS_PER_PLAYER && opponent.getQuestionsAnswered() == TOTAL_QUESTIONS_PER_PLAYER;
    }

    private void determineGameOutcome(MultiplayerSession session) {
        log.debug("Determining game outcome for sessionId: {}", session.getId());
        List<SessionPlayer> players = session.getSessionPlayers();

        Long player1Id = players.get(0).getId();
        Long player2Id = players.get(1).getId();
        
        int score1 = players.get(0).getScore();
        int score2 = players.get(1).getScore();

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
        statsService.updateGameStats(players.get(0).getPlayer(), players.get(1).getPlayer(), result);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
        log.debug("GameWonEvent published for sessionId: {}", session.getId());
    }

    private void sendGameOverNotifications(MultiplayerSession session) {
        log.debug("Sending game over notifications for sessionId: {}", session.getId());
        if (session.getIsTie()) {
            notificationService.sendTieNotifications(session.getSessionPlayers(), session.getId());
        } else {
            Player loserPlayer = session.getSessionPlayerById(session.getLoserId()).getPlayer();
            Player winnerPlayer = session.getSessionPlayerById(session.getWinnerId()).getPlayer();
            
            notificationService.sendGameWonNotification(session.getWinnerId(), loserPlayer.getDisplayName(), session.getId());
            notificationService.sendGameLostNotification(session.getLoserId(), winnerPlayer.getDisplayName(), session.getId());
            log.debug("Sent game won notification to {} and game lost notification to {}", session.getWinnerId(), session.getLoserId());
        }
    }
    
    public void updateSessionQuestionsAndCategory(MultiplayerSession session, List<Question> questions, String selectedCategory) {
        log.info("Updating session questions and category for sessionId: {}", session.getId());
        
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
            log.debug("Added question id: {}", question.getId());
        }
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
        
        session.getSessionPlayerById(playerId).setHasAcknowledgedGameOver(true);
        multiplayerSessionRepository.save(session);
        log.debug("Marked acknowledgement as true for playerId: {} and saved", playerId);
    }

    @Transactional
    public void handleGiveUp(Long sessionId, Long playerId) {
        log.info("Handling give-up for sessionId: {} and playerId: {}", sessionId, playerId);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);

        session.getSessionPlayerById(playerId).setGivenUp(true);
        session.setStatus(GameStatus.COMPLETED);
        log.debug("Session status set to COMPLETED for sessionId: {}", sessionId);

        session.setLoserId(playerId);
        log.debug("Player with ID {} marked as loser for sessionId: {}", playerId, sessionId);
        session.setWinnerId(session.getSessionPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .map(SessionPlayer::getId)
                .orElseThrow(() -> {
                    log.error("Opponent not found for sessionId: {}", sessionId);
                    return new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session");
                }));

        multiplayerSessionRepository.save(session);
        
        Player loserPlayer = session.getSessionPlayerById(session.getLoserId()).getPlayer();
        Player winnerPlayer = session.getSessionPlayerById(session.getWinnerId()).getPlayer();
        
        statsService.updateGameStats(loserPlayer, winnerPlayer, GameResult.WIN_PLAYER2);
        
        notificationService.sendGameWonNotification(winnerPlayer.getId(), loserPlayer.getDisplayName(), sessionId);
        notificationService.sendGameLostNotification(loserPlayer.getId(), winnerPlayer.getDisplayName(), sessionId);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
    }
}
