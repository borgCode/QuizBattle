package org.borg.backend.game.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.GameStateResponse;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;
import org.borg.backend.game.shared.enums.GameResult;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.events.AchievementEvents;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.StatsService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.GameException;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.Map.Entry;

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

    public GameStateResponse getGameState(long sessionId, long playerId) {
        log.debug("Fetching game state for sessionId: {}, playerId: {}", sessionId, playerId);
        MultiplayerSession multiplayerSession = multiplayerSessionService.getSessionById(sessionId);

        boolean playerExistsInSession = multiplayerSession.getPlayers().stream()
                .anyMatch(player -> player.getId().equals(playerId));

        if (!playerExistsInSession) {
            log.warn("Access denied for playerId: {} in sessionId: {}", playerId, sessionId);
            throw new AccessDeniedException("Not authorized to access this game session");
        }

        Long playerWhoGaveUp = multiplayerSession.getPlayerHasGivenUp().entrySet().stream()
                .filter(Entry::getValue)
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);

        log.debug("Game state fetched successfully for sessionId: {}, playerId: {}", sessionId, playerId);
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
        log.debug("Updating game state for sessionId: {}, playerId: {}, questionId: {}, isCorrect: {}",
                sessionId, playerId, questionId, isCorrect);
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);
        
        if (isCorrect) {
            log.debug("Updating score for playerId: {}", playerId);
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
            log.debug("PlayerId: {} new score: {}", playerId, playerScore + 1);
        }

        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);
        log.debug("PlayerId: {} questions answered updated to: {}", playerId, questionsAnswered.get(playerId));

        session.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, session.getQuestionsAnswered().get(playerId) - 1, isCorrect));

        Player opponent = session.getPlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session"));

        if (isGameComplete(questionsAnswered)) {
            log.debug("Game is complete for sessionId: {}", sessionId);
            session.setStatus(GameStatus.COMPLETED);
            determineGameOutcome(session);
            sendGameOverNotifications(session);
        } else {

            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);

            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    session.setCurrentPlayerTurn(opponent);
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

    private boolean isGameComplete(Map<Long, Integer> questionsAnswered) {
        return questionsAnswered.values().stream()
                .allMatch(count -> count == MultiplayerGameConstants.TOTAL_QUESTIONS_PER_PLAYER);
    }

    private void determineGameOutcome(MultiplayerSession session) {
        log.debug("Determining game outcome for sessionId: {}", session.getId());
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

        log.info("Game outcome determined for sessionId: {}, result: {}", session.getId(), result);
        statsService.updateGameStats(players.get(0), players.get(1), result);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
        log.debug("GameWonEvent published for sessionId: {}", session.getId());
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
        log.debug("Finding playerId: {} in sessionId: {}", playerId, session.getId());
        return session.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player not found for " + playerId));
    }

    public void updateSessionQuestionsAndCategory(MultiplayerSession session, List<Question> questions, String selectedCategory) {
        session.getQuestionIds().clear();
        for (Question question : questions) {
            session.getQuestionIds().add(question.getId());
        }
        session.getPlayedCategories().add(selectedCategory);

        session.getRoundCategories().add(selectedCategory);
        multiplayerSessionRepository.save(session);
    }
    
    public void acknowledgeGameOver(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);
        
        session.getPlayerAcknowledgment().put(playerId, true);
        multiplayerSessionRepository.save(session);
    }

    @Transactional
    public void handleGiveUp(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionService.getSessionById(sessionId);

        session.getPlayerHasGivenUp().put(playerId, true);
        session.setStatus(GameStatus.COMPLETED);

        session.setLoserId(playerId);
        session.setWinnerId(session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .map(Player::getId)
                .orElseThrow(() -> new GameException(BusinessErrorCodes.INVALID_SESSION_STATE, "Opponent not found in session")));

        multiplayerSessionRepository.save(session);
        
        Player loserPlayer = findPlayerInSession(session, session.getLoserId());
        Player winnerPlayer = findPlayerInSession(session, session.getWinnerId());
        
        statsService.updateGameStats(loserPlayer, winnerPlayer, GameResult.WIN_PLAYER2);
        
        notificationService.sendGameWonNotification(winnerPlayer.getId(), loserPlayer.getDisplayName(), sessionId);
        notificationService.sendGameLostNotification(loserPlayer.getId(), winnerPlayer.getDisplayName(), sessionId);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));
    }
}
