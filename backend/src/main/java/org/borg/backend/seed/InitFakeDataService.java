package org.borg.backend.seed;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.game.multiplayer.model.MultiplayerSession;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.game.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;
import org.borg.backend.game.shared.enums.GameStatus;
import org.borg.backend.game.shared.model.Question;
import org.borg.backend.player.model.CategoryStats;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.social.chat.model.Conversation;
import org.borg.backend.social.chat.model.Message;
import org.borg.backend.social.chat.repository.ConversationRepository;
import org.borg.backend.social.chat.repository.MessageRepository;
import org.borg.backend.social.friendship.model.Friendship;
import org.borg.backend.social.friendship.model.FriendshipStatus;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.notification.model.Notification;
import org.borg.backend.social.notification.model.NotificationType;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitFakeDataService {
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker(new Random(32));
    private final MultiplayerSessionRepository multiplayerSessionRepository;
    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final PlayerService playerService;
    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;

    public enum Outcome {
        PLAYER1_WINS,
        PLAYER2_WINS,
        TIE;

    }
    private Player generateRandomPlayer(Role role, Set<String> existingUsernames) {
        String username;
        do {
            username = faker.name().username();
        } while (existingUsernames.contains(username));
        existingUsernames.add(username);

        return Player.builder()
                .username(username)
                .displayName(faker.name().name())
                .roles(List.of(role))
                .stats(new Stats())
                .password(passwordEncoder.encode("password"))
                .accountLocked(false)
                .enabled(true)
                .build();
    }
    public List<Player> generateRandomPlayers(Role role, int count) {
        Set<String> existingUsernames = new HashSet<>();
        return IntStream.range(0, count)
                .mapToObj(i -> generateRandomPlayer(role, existingUsernames))
                .collect(Collectors.toList());
    }

    public Question generateRandomQuestion() {
        String correctAnswer = faker.lorem().word();

        List<String> options = IntStream.range(0, 4)
                .mapToObj(i -> i == 0
                        ? correctAnswer
                        : faker.lorem().word())
                .collect(Collectors.toList());

        return Question.builder()
                .category(getRandomCategory())
                .question(faker.lorem().sentence())
                .options(options)
                .correctAnswer(correctAnswer)
                .build();
    }

    public List<Question> generateRandomQuestions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> generateRandomQuestion())
                .collect(Collectors.toList());
    }

    public String getRandomCategory() {
        List<String> categories = List.of(
                "Animals",
                "Books",
                "Film",
                "General Knowledge",
                "Geography",
                "History",
                "Japanese Anime & Manga",
                "Music",
                "Science & Nature",
                "Sports",
                "Television"
        );

        int randomIndex = faker.random().nextInt(categories.size());
        return categories.get(randomIndex);
    }

    public MultiplayerSession generateCompletedMultiplayerSessions(Player player1, Player player2) {
        Long currentPlayerTurnId = faker.random().nextBoolean() ? player1.getId() : player2.getId();

        MultiplayerSession session = new MultiplayerSession(player1, player2, currentPlayerTurnId);
        session.setStatus(GameStatus.COMPLETED);
        session.setCurrentQuestionIndex(18);

        Set<String> categories = new HashSet<>();
        List<String> roundCategories = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String randomCategory = getRandomCategory();
            categories.add(randomCategory);
            roundCategories.add(randomCategory);
        }

        session.setPlayedCategories(categories);
        session.setRoundCategories(roundCategories);

        SessionPlayer sessionPlayer1 = session.getSessionPlayers().get(0);
        SessionPlayer sessionPlayer2 = session.getSessionPlayers().get(1);

        setupBasePlayerData(sessionPlayer1);
        setupBasePlayerData(sessionPlayer2);

        switch (getRandomOutcome()) {
            case PLAYER1_WINS -> setupWinnerLoser(session, sessionPlayer1, sessionPlayer2);
            case PLAYER2_WINS -> setupWinnerLoser(session, sessionPlayer2, sessionPlayer1);
            case TIE -> setupTie(session, sessionPlayer1, sessionPlayer2);
        }

        return multiplayerSessionRepository.save(session);
    }

    private void setupBasePlayerData(SessionPlayer sessionPlayer) {
        sessionPlayer.setQuestionsAnswered(18);
        sessionPlayer.setHasAcknowledgedGameOver(true);
        sessionPlayer.setGivenUp(false);

        Set<PlayerQuestionResult> results = new HashSet<>();
        for (int i = 0; i < 18; i++) {
            results.add(PlayerQuestionResult.builder()
                    .questionId((long) (i + 1))
                    .questionIndex(i)
                    .build());
        }
        sessionPlayer.setQuestionResults(results);
    }

    public Outcome getRandomOutcome() {
        Outcome[] outcomes = Outcome.values();
        return outcomes[random.nextInt(outcomes.length)];
    }

    private void setupWinnerLoser(MultiplayerSession session, SessionPlayer winner, SessionPlayer loser) {
        setPlayerResults(winner.getQuestionResults(), 15);
        setPlayerResults(loser.getQuestionResults(), 9);

        session.setWinnerId(winner.getPlayer().getId());
        session.setLoserId(loser.getPlayer().getId());
        session.setIsTie(false);
    }

    private void setupTie(MultiplayerSession session, SessionPlayer player1, SessionPlayer player2) {
        setPlayerResults(player1.getQuestionResults(), 12);
        setPlayerResults(player2.getQuestionResults(), 12);

        session.setWinnerId(null);
        session.setLoserId(null);
        session.setIsTie(true);
    }

    private void setPlayerResults(Set<PlayerQuestionResult> questionResults, int correctAnswers) {
        int count = 0;
        for (PlayerQuestionResult result : questionResults) {
            result.setCorrect(count < correctAnswers);
            count++;
        }
    }

    public void generateCompletedMultiplayerGames(List<Player> players, int count) {
        IntStream.range(0, count)
                .mapToObj(i -> {
                    Collections.shuffle(players, random);

                    Player player1 = players.get(0);
                    Player player2 = players.get(1);

                    return generateCompletedMultiplayerSessions(player1, player2);
                })
                .collect(Collectors.toList());
    }

    public MultiplayerSession generateActiveMultiplayerSession(Player player1, Player player2) {
        Long currentPlayerTurnId = faker.random().nextBoolean() ? player1.getId() : player2.getId();

        MultiplayerSession session = new MultiplayerSession(player1, player2, currentPlayerTurnId);
        session.setStatus(GameStatus.ACTIVE);

        int[] validQuestionCounts = {0, 3, 6, 9, 12, 15};

        int player1QuestionsIndex = random.nextInt(validQuestionCounts.length);
        int player1Questions = validQuestionCounts[player1QuestionsIndex];

        int player2QuestionsIndex;
        if (player1QuestionsIndex == 0) {
            player2QuestionsIndex = random.nextInt(2);
        } else if (player1QuestionsIndex == validQuestionCounts.length - 1) {
            player2QuestionsIndex = player1QuestionsIndex - random.nextInt(2);
        } else {
            player2QuestionsIndex = player1QuestionsIndex + (random.nextInt(3) - 1);
        }
        int player2Questions = validQuestionCounts[player2QuestionsIndex];

        int currentQuestionIndex = Math.max(player1Questions, player2Questions);
        session.setCurrentQuestionIndex(currentQuestionIndex);

        int completeCategorySets = currentQuestionIndex / 3;

        Set<String> playedCategories = new HashSet<>();
        List<String> roundCategories = new ArrayList<>();

        for (int i = 0; i < completeCategorySets; i++) {
            String randomCategory = getRandomCategory();
            playedCategories.add(randomCategory);
            roundCategories.add(randomCategory);
        }

        session.setPlayedCategories(playedCategories);
        session.setRoundCategories(roundCategories);

        SessionPlayer sessionPlayer1 = session.getSessionPlayers().get(0);
        SessionPlayer sessionPlayer2 = session.getSessionPlayers().get(1);

        setupActivePlayerData(sessionPlayer1, player1Questions);
        setupActivePlayerData(sessionPlayer2, player2Questions);

        setPartialPlayerResults(sessionPlayer1.getQuestionResults(), random.nextInt(player1Questions + 1), player1Questions);
        setPartialPlayerResults(sessionPlayer2.getQuestionResults(), random.nextInt(player2Questions + 1), player2Questions);

        return multiplayerSessionRepository.save(session);
    }

    private void setupActivePlayerData(SessionPlayer sessionPlayer, int questionsAnswered) {
        sessionPlayer.setQuestionsAnswered(questionsAnswered);
        sessionPlayer.setHasAcknowledgedGameOver(false);
        sessionPlayer.setGivenUp(false);

        Set<PlayerQuestionResult> results = new HashSet<>();
        for (int i = 0; i < questionsAnswered; i++) {
            results.add(PlayerQuestionResult.builder()
                    .questionId((long) (i + 1))
                    .questionIndex(i)
                    .build());
        }
        sessionPlayer.setQuestionResults(results);
    }

    private void setPartialPlayerResults(Set<PlayerQuestionResult> questionResults, int correctAnswers, int totalAnswered) {
        List<PlayerQuestionResult> resultsList = new ArrayList<>(questionResults);
        Collections.shuffle(resultsList, random);

        int count = 0;
        for (PlayerQuestionResult result : resultsList) {
            result.setCorrect(count < correctAnswers);
            count++;
        }
    }

    public void generateActiveMultiplayerGames(List<Player> players, int count) {
        Set<String> playerPairs = new HashSet<>();
        List<MultiplayerSession> sessions = new ArrayList<>();

        int attempts = 0;
        int maxAttempts = count * 10;

        while (sessions.size() < count && attempts < maxAttempts) {
            Collections.shuffle(players, random);
            Player player1 = players.get(0);
            Player player2 = players.get(1);

            Long id1 = player1.getId();
            Long id2 = player2.getId();
            String pairKey = (id1 < id2) ? id1 + "-" + id2 : id2 + "-" + id1;

            if (!playerPairs.contains(pairKey)) {
                playerPairs.add(pairKey);
                sessions.add(generateActiveMultiplayerSession(player1, player2));
            }

            attempts++;
        }

        if (sessions.size() < count) {
            throw new IllegalStateException("Could not generate requested number of unique game sessions. " +
                    "Generated " + sessions.size() + " out of " + count + " requested sessions.");
        }
    }

    @Transactional
    public void generatePlayerStats(List<Player> players) {
        for (Player player : players) {
            playerRepository.save(generatePlayerStat(player));
        }
    }

    private Player generatePlayerStat(Player player) {
        List<String> categories = List.of(
                "Animals",
                "Books",
                "Film",
                "General Knowledge",
                "Geography",
                "History",
                "Japanese Anime & Manga",
                "Music",
                "Science & Nature",
                "Sports",
                "Television"
        );
        for (int i = 0; i < 11; i++) {
            CategoryStats categoryStats = new CategoryStats(categories.get(i));
            categoryStats.setQuestionsAnswered(faker.number().numberBetween(4000, 6000));
            categoryStats.setCorrect(faker.number().numberBetween(2000, 3500));
            player.getStats().getCategoryStats().put(categories.get(i), categoryStats);
        }
        player.getStats().setNumOfWins(faker.number().numberBetween(10000, 15000));
        player.getStats().setNumOfLosses(faker.number().numberBetween(10000, 15000));
        player.getStats().setNumOfGames(player.getStats().getNumOfWins() + player.getStats().getNumOfLosses());

        return player;
    }

    @Transactional
    public void generateMessages(int count, Long receiverId, Long senderId) {
        Player player1 = playerService.getPlayerById(senderId);
        Player player2 = playerService.getPlayerById(receiverId);
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .player1(player1)
                .player2(player2)
                .build());
        
        
        Message latestMessage = null;
        for (int i = 0; i < count; i++) {
            Message message = generateMessage(receiverId, senderId, conversation);
            messageRepository.save(message);
            latestMessage = message; 
        }
        conversation.setLatestMessage(latestMessage);
        conversationRepository.save(conversation);
    }

    private Message generateMessage(Long receiverId, Long senderId, Conversation conversation) {
        return Message.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .conversation(conversation)
                .read(false)
                .content(faker.lorem().sentence(3)).build();
    }

    public void generateFriendsForPlayer(int count, long playerId) {
        Player targetPlayer = playerRepository.findById(playerId)
                .orElseThrow();
        List<Player> availablePlayers = playerRepository.findAll().stream()
                .filter(player -> !player.getId().equals(playerId))
                .toList();

        List<Friendship> friendships = new ArrayList<>();
        
        for (int i = 0; i < Math.min(count, availablePlayers.size()); i++) {
            Player randomPlayer = getRandomUnusedPlayer(availablePlayers, friendships);

            if (randomPlayer == null) {
                break; 
            }
            
            LocalDate friendshipDate = faker.date().birthday(0, 1)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            FriendshipStatus status = getRandomFriendshipStatus();
            
            boolean isTargetPlayer1 = random.nextBoolean();
            Friendship friendship = isTargetPlayer1
                    ? new Friendship(targetPlayer, randomPlayer, friendshipDate, status)
                    : new Friendship(randomPlayer, targetPlayer, friendshipDate, status);

            friendships.add(friendship);
        }
        
        friendshipRepository.saveAll(friendships);
    }

    private Player getRandomUnusedPlayer(List<Player> availablePlayers, List<Friendship> existingFriendships) {
        List<Player> unusedPlayers = availablePlayers.stream()
                .filter(player -> !isPlayerInFriendships(player, existingFriendships))
                .toList();

        if (unusedPlayers.isEmpty()) {
            return null;
        }

        return unusedPlayers.get(random.nextInt(unusedPlayers.size()));
    }

    private boolean isPlayerInFriendships(Player player, List<Friendship> friendships) {
        return friendships.stream()
                .anyMatch(friendship ->
                        friendship.getPlayer1().getId().equals(player.getId()) ||
                                friendship.getPlayer2().getId().equals(player.getId()));
    }

    private FriendshipStatus getRandomFriendshipStatus() {
        FriendshipStatus[] statuses = FriendshipStatus.values();
        return statuses[random.nextInt(statuses.length)];
    }

    public void generateNotificationsForPlayer(int count, long playerId) {
        List<Notification> notifications = new ArrayList<>();
        List<Player> otherPlayers = playerRepository.findAll().stream()
                .filter(p -> !p.getId().equals(playerId))
                .toList();

        if (otherPlayers.isEmpty()) {
            throw new IllegalStateException("Need at least one other player to generate notifications");
        }

        for (int i = 0; i < count; i++) {
            notifications.add(generateRandomNotification(playerId, otherPlayers));
        }

        notificationRepository.saveAll(notifications);
    }

    private Notification generateRandomNotification(Long playerId, List<Player> otherPlayers) {
        NotificationType type = getRandomNotificationType();
        Player otherPlayer = getRandomPlayer(otherPlayers);

        return Notification.builder()
                .recipientId(playerId)
                .senderId(needsSender(type) ? otherPlayer.getId() : null)
                .type(type)
                .message(generateMessage(type, otherPlayer))
                .pendingSessionId(isMatchRelated(type) ? faker.random().nextLong(1000) : null)
                .startedSessionId(isGameRelated(type) ? faker.random().nextLong(1000) : null)
                .isRead(random.nextBoolean())
                .isArchived(random.nextInt(100) < 20) 
                .hiddenByBlock(random.nextInt(100) < 10) 
                .createdAt(generateRandomTimestamp())
                .build();
    }

    private NotificationType getRandomNotificationType() {
        NotificationType[] types = NotificationType.values();
        return types[random.nextInt(types.length)];
    }

    private Player getRandomPlayer(List<Player> players) {
        return players.get(random.nextInt(players.size()));
    }

    private boolean needsSender(NotificationType type) {
        return switch (type) {
            case FRIEND_REQUEST, FRIEND_ACCEPTED, MATCH_REQUEST, REMATCH_REQUEST -> true;
            default -> false;
        };
    }

    private boolean isMatchRelated(NotificationType type) {
        return switch (type) {
            case MATCH_REQUEST, REMATCH_REQUEST -> true;
            default -> false;
        };
    }

    private boolean isGameRelated(NotificationType type) {
        return switch (type) {
            case GAME_WON, GAME_LOST, GAME_TIED -> true;
            default -> false;
        };
    }

    private String generateMessage(NotificationType type, Player otherPlayer) {
        return switch (type) {
            case FRIEND_REQUEST -> otherPlayer.getDisplayName() + " sent you a friend request!";
            case FRIEND_ACCEPTED -> otherPlayer.getDisplayName() + " accepted your friend request!";
            case MATCH_REQUEST -> otherPlayer.getDisplayName() + " requested a match against you!";
            case MATCH_ACCEPTED -> "Your match request against " + otherPlayer.getDisplayName() + " was accepted!";
            case MATCH_DECLINED -> "Your match request against " + otherPlayer.getDisplayName() + " was declined!";
            case REMATCH_REQUEST -> otherPlayer.getDisplayName() + " requested a rematch against you!";
            case REMATCH_ACCEPTED -> "Your rematch request against " + otherPlayer.getDisplayName() + " was accepted!";
            case REMATCH_DECLINED -> "Your rematch request against " + otherPlayer.getDisplayName() + " was declined!";
            case GAME_WON -> "You won your match against " + otherPlayer.getDisplayName() + "!";
            case GAME_LOST -> "You lost your match against " + otherPlayer.getDisplayName() + "!";
            case GAME_TIED -> "Your match against " + otherPlayer.getDisplayName() + " was tied!";
        };
    }

    private Instant generateRandomTimestamp() {
        long daysToSubtract = random.nextInt(30);
        long hoursToSubtract = random.nextInt(24);
        long minutesToSubtract = random.nextInt(60);

        return Instant.now()
                .minus(daysToSubtract, ChronoUnit.DAYS)
                .minus(hoursToSubtract, ChronoUnit.HOURS)
                .minus(minutesToSubtract, ChronoUnit.MINUTES);
    }
}
