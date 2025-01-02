package org.borg.backend.multiplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.achievement.events.AchievementEvents;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.enums.GameStatus;
import org.borg.backend.common.exceptions.GameException;
import org.borg.backend.multiplayer.dto.GameStateResponse;
import org.borg.backend.multiplayer.dto.MultiplayerSessionDTO;
import org.borg.backend.multiplayer.dto.RematchRequest;
import org.borg.backend.multiplayer.dto.RematchResponse;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.model.PendingSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.multiplayer.repository.PendingSessionRepository;
import org.borg.backend.multiplayer.util.MultiplayerGameConstants;
import org.borg.backend.notification.service.NotificationService;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.question.dto.PlayerQuestionResult;
import org.borg.backend.question.dto.Question;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiplayerService {

    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final NotificationService notificationService;
    private final PendingSessionRepository pendingSessionRepository;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GameStateResponse getGameState(Long sessionId) {
        MultiplayerSession multiplayerSession = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));


        Long playerWhoGaveUp = multiplayerSession.getPlayerHasGivenUp().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
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

        //Increment score if answer was correct
        if (isCorrect) {
            Map<Long, Integer> scores = session.getScore();
            Integer playerScore = scores.getOrDefault(playerId, 0);
            scores.put(playerId, playerScore + 1);
        }

        //Update questions answered
        Map<Long, Integer> questionsAnswered = session.getQuestionsAnswered();
        questionsAnswered.put(playerId, questionsAnswered.get(playerId) + 1);

        //Update which question out of the 18 is correct
        session.getQuestionResults().add(new PlayerQuestionResult(playerId, questionId, session.getQuestionsAnswered().get(playerId) - 1, isCorrect));
        //Find opponent in session
        Player opponent = session.getPlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No opponent found in session"));

        if (isGameComplete(questionsAnswered)) {
            session.setStatus(GameStatus.COMPLETED);
            determineGameOutcome(session);
            sendGameOverNotifications(session);

        } else {
            //Update question index to manage turns
            session.setCurrentQuestionIndex((session.getCurrentQuestionIndex() + 1) % MultiplayerGameConstants.QUESTIONS_PER_ROUND);

            //Determine if it's the opponent's turn
            if (session.getCurrentQuestionIndex() == 0) {
                if (questionsAnswered.get(playerId) > questionsAnswered.get(opponent.getId())) {
                    session.setCurrentPlayerTurn(opponent);
                } else {
                    session.getQuestionIds().clear();
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
        Stats player1Stats = players.get(0).getStats();
        Stats player2Stats = players.get(1).getStats();

        Long player1Id = players.get(0).getId();
        Long player2Id = players.get(1).getId();

        Map<Long, Integer> scores = session.getScore();
        int score1 = scores.get(player1Id);
        int score2 = scores.get(player2Id);


        if (score1 > score2) {
            session.setWinnerId(player1Id);
            session.setLoserId(player2Id);
            session.setIsTie(false);

            player1Stats.incrementWins();
            player2Stats.incrementLosses();


        } else if (score1 < score2) {
            session.setWinnerId(player2Id);
            session.setLoserId(player1Id);
            session.setIsTie(false);

            player1Stats.incrementLosses();
            player2Stats.incrementWins();
        } else {
            session.setIsTie(true);

            player1Stats.incrementTies();
            player2Stats.incrementTies();
        }


        players.get(0).setStats(player1Stats);
        players.get(1).setStats(player2Stats);

        playerRepository.saveAll(players);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));

    }

    private void sendGameOverNotifications(MultiplayerSession session) {
        if (session.getIsTie()) {
            notificationService.sendTieNotifications(session);
        } else {
            notificationService.sendGameWonNotification(session);
            notificationService.sendGameLostNotification(session);
        }
    }

    public void updateSessionQuestionsAndCategory(Long sessionId, List<Question> questions, String selectedCategory) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

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
    public void requestRematch(RematchRequest rematchRequest) {


        log.warn("Rematch request: {}", rematchRequest);
        MultiplayerSession session = multiplayerSessionRepository.findById(rematchRequest.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        if (session.getStatus().equals(GameStatus.ACTIVE)) {
            throw new GameException(BusinessErrorCodes.GAME_ALREADY_ONGOING);
        }

        Long playerId = rematchRequest.getPlayerId();

        Player sendingPlayer = session.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player not found in session"));

        Player opponentPlayer = session.getPlayers().stream()
                .filter(player -> !player.equals(sendingPlayer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Opponent not found in session"));

        if (multiplayerSessionRepository.checkIfOngoingSessionExists(sendingPlayer, opponentPlayer, GameStatus.ACTIVE)) {
            throw new GameException(BusinessErrorCodes.GAME_ALREADY_ONGOING);
        }

        if (pendingSessionRepository.existsByRequestingPlayerIdAndOpponentId(sendingPlayer.getId(), opponentPlayer.getId())) {
            throw new GameException(BusinessErrorCodes.REMATCH_REQUEST_ALREADY_SENT);
        }
        
        PendingSession pendingSession = pendingSessionRepository.findByRequestingPlayerIdAndOpponentId(opponentPlayer.getId(), sendingPlayer.getId());
        log.warn("Pending session is: " + pendingSession);

        if (pendingSession != null) {

            log.warn("Has pending session");

            Long newSessionId = createRematchSession(session);
            log.warn("new session id: {}", newSessionId);


            //Delete original notification
            notificationService.deleteMatchRequestNotification(playerId, pendingSession.getId());


            pendingSessionRepository.delete(pendingSession);


            notificationService.sendRematchStartedNotification(playerId, opponentPlayer.getDisplayName(), newSessionId);
            notificationService.sendRematchStartedNotification(opponentPlayer.getId(), sendingPlayer.getDisplayName(), newSessionId);


        } else {
            log.warn("No existing session, creating new pending");

            PendingSession newSession = pendingSessionRepository.save(new PendingSession(playerId, opponentPlayer.getId()));
            notificationService.sendRematchRequestNotification(opponentPlayer.getId(), newSession.getId(), sendingPlayer.getDisplayName(), sendingPlayer.getId());
        }
    }

    private Long createRematchSession(MultiplayerSession session) {
        List<Player> players = session.getPlayers();

        Player startingPlayer = Math.random() < 0.5 ? players.get(0) : players.get(1);


        MultiplayerSession multiplayerSession = multiplayerSessionRepository.save(
                new MultiplayerSession(players.get(0), players.get(1), startingPlayer));
        return multiplayerSession.getId();
    }

    @Transactional
    public Long handleRematchAccept(RematchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));
        Long newSessionId = createMultiplayerSession(pendingSession);

        notificationService.sendRematchAcceptedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId(), newSessionId);
        return newSessionId;
    }

    @Transactional
    public void handleRematchReject(RematchResponse response) {
        PendingSession pendingSession = pendingSessionRepository.findById(response.getPendingSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        notificationService.sendRematchRejectedNotification(response.getOriginalSenderId(), response.getPlayerDisplayName(), response.getNotificationId());
        pendingSessionRepository.delete(pendingSession);
    }

    private Long createMultiplayerSession(PendingSession pendingSession) {
        Player player1 = playerRepository.findById(pendingSession.getRequestingPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Requesting player not found"));
        Player player2 = playerRepository.findById(pendingSession.getOpponentId())
                .orElseThrow(() -> new NoSuchElementException("Opponent not found"));

        //Randomly choose who starts

        Player startingPlayer = Math.random() < 0.5 ? player1 : player2;

        MultiplayerSession session = multiplayerSessionRepository.save(
                new MultiplayerSession(player1, player2, startingPlayer));
        return session.getId();

    }


    @Transactional
    public void handleGiveUp(Long sessionId, Long playerId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found!"));

        session.getPlayerHasGivenUp().put(playerId, true);
        session.setStatus(GameStatus.COMPLETED);
        multiplayerSessionRepository.save(session);

        List<Player> players = session.getPlayers();

        if (players.get(0).getId().equals(playerId)) {
            players.get(0).getStats().incrementLosses();
            players.get(1).getStats().incrementWins();
        } else {
            players.get(1).getStats().incrementLosses();
            players.get(0).getStats().incrementWins();
        }


        playerRepository.saveAll(players);

        session.setLoserId(playerId);
        session.setWinnerId(session.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .map(Player::getId)
                .orElseThrow(() -> new IllegalStateException("Player not found in session")));

        notificationService.sendGameWonNotification(session);
        notificationService.sendGameLostNotification(session);

        applicationEventPublisher.publishEvent(new AchievementEvents.GameWonEvent(session.getWinnerId()));

    }
}
            
