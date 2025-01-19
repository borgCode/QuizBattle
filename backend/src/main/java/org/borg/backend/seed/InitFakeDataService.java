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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    

    public enum Outcome {
        PLAYER1_WINS,
        PLAYER2_WINS,
        TIE
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
    
    private  void setPlayerResults(Set<PlayerQuestionResult> questionResults, int correctAnswers) {
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
            if (count < correctAnswers) {
                result.setCorrect(true);
            } else {
                result.setCorrect(false);
            }
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
        for (int i = 0; i < 1000; i++) {
            playerRepository.save(generatePlayerStat(players.get(i)));
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
}
