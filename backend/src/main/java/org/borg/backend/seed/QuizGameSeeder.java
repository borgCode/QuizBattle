package org.borg.backend.seed;

import com.github.javafaker.Faker;
import org.borg.backend.multiplayer.enums.GameStatus;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.borg.backend.player.model.CategoryStats;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.PlayerRepository;
import org.borg.backend.player.model.Stats;
import org.borg.backend.question.PlayerQuestionResult;
import org.borg.backend.question.Question;
import org.borg.backend.question.QuestionRepository;
import org.borg.backend.role.Role;
import org.borg.backend.role.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizGameSeeder {
    private final Faker faker;
    private final Random random;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final RoleRepository roleRepository;
    private final MultiplayerSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public QuizGameSeeder(
            PlayerRepository playerRepository,
            QuestionRepository questionRepository,
            RoleRepository roleRepository,
            MultiplayerSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.questionRepository = questionRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.faker = new Faker();
        this.random = new Random();
    }
    

    @Transactional
    public void seedDatabase(int numPlayers, int numQuestions, int numSessions) {

        List<String> categories = Arrays.asList("History", "Science", "Geography", "Entertainment", "Sports", "Literature", "Culture");
        
        // Create roles first
        List<Role> roles = createRoles();

        // Create players
        List<Player> players = createPlayers(numPlayers, roles, categories);

        // Create questions
        List<Question> questions = createQuestions(numQuestions, categories);

        // Create multiplayer sessions with more sessions per player
        createMultiplayerSessions(numSessions, players, questions);

        System.out.println("Database seeded successfully!");
    }

    private List<Role> createRoles() {
        List<String> roleNames = List.of("USER");
        return roleNames.stream()
                .map(name -> {
                    Role role = new Role();
                    role.setName(name);
                    return roleRepository.save(role);
                })
                .collect(Collectors.toList());
    }

    private List<Player> createPlayers(int count, List<Role> roles, List<String> categories) {
        Role userRole = roles.stream()
                .filter(r -> r.getName().equals("USER"))
                .findFirst()
                .orElseThrow();

        List<Player> players = new ArrayList<>();

        // Create test user
        Player testUser = new Player();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setDisplayName("Test User");
        testUser.setAccountLocked(false);
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singletonList(userRole));

        Stats testStats = new Stats();
        testStats.setPlayer(testUser);
        testStats.setNumOfGames(10);
        testStats.setNumOfWins(5);
        testStats.setNumOfLosses(5);

        Map<String, CategoryStats> testCategoryStatsMap = new HashMap<>();
        for (String category : categories) {
            CategoryStats categoryStats = new CategoryStats();
            categoryStats.setStats(testStats);
            categoryStats.setCorrect(random.nextInt(10)); // Example: Random wins for the category
            categoryStats.setQuestionsAnswered(random.nextInt(10)); // Example: Random losses for the category
            testCategoryStatsMap.put(category, categoryStats);
        }

        testStats.setCategoryStats(testCategoryStatsMap);
        testUser.setStats(testStats);

        players.add(playerRepository.save(testUser));

        for (int i = 0; i < count; i++) {
            Player player = new Player();
            player.setUsername(faker.name().username() + random.nextInt(1000));
            player.setPassword("$2a$10$" + faker.crypto().sha256().substring(0, 50));
            player.setDisplayName(faker.superhero().name());
            player.setAccountLocked(false);
            player.setEnabled(true);
            player.setRoles(Collections.singletonList(userRole));

            Stats stats = new Stats();
            stats.setPlayer(player);
            stats.setNumOfGames(0); // Initialize with 0 games
            stats.setNumOfWins(0); // Initialize with 0 wins
            stats.setNumOfLosses(0); // Initialize with 0 losses

            Map<String, CategoryStats> categoryStatsMap = new HashMap<>();
            for (String category : categories) {
                CategoryStats categoryStats = new CategoryStats();
                categoryStats.setStats(stats);
                categoryStats.setCorrect(random.nextInt(10)); // Example: Random wins for the category
                categoryStats.setQuestionsAnswered(random.nextInt(10)); // Example: Random losses for the category
                categoryStatsMap.put(category, categoryStats);
            }

            stats.setCategoryStats(categoryStatsMap);
            player.setStats(stats);

            players.add(playerRepository.save(player));
        }
        return players;
    }

    private List<Question> createQuestions(int count, List<String> categories) {
        
        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Question question = new Question();
            question.setCategory(categories.get(random.nextInt(categories.size())));
            question.setQuestion(faker.lorem().sentence() + "?");

            List<String> options = new ArrayList<>();
            String correctAnswer = faker.lorem().word();
            options.add(correctAnswer);

            for (int j = 0; j < 3; j++) {
                options.add(faker.lorem().word());
            }

            Collections.shuffle(options);
            question.setOptions(options);
            question.setCorrectAnswer(correctAnswer);

            questions.add(questionRepository.save(question));
        }

        return questions;
    }

    private void createMultiplayerSessions(int count, List<Player> allPlayers, List<Question> allQuestions) {
        // Ensure each player participates in multiple sessions
        for (Player player : allPlayers) {
            // Create 2-5 sessions for each player
            int sessionsForPlayer = random.nextInt(4) + 2;

            for (int i = 0; i < sessionsForPlayer; i++) {
                MultiplayerSession session = new MultiplayerSession();

                // Add the current player and 1-3 random other players
                List<Player> sessionPlayers = new ArrayList<>();
                sessionPlayers.add(player);

                List<Player> otherPlayers = allPlayers.stream()
                        .filter(p -> !p.equals(player))
                        .collect(Collectors.toList());

                int additionalPlayers = random.nextInt(1) + 2;
                sessionPlayers.addAll(getRandomSublist(otherPlayers, additionalPlayers));

                session.setPlayers(sessionPlayers);
                session.setCurrentPlayerTurn(sessionPlayers.get(random.nextInt(sessionPlayers.size())));
                session.setCurrentQuestionIndex(random.nextInt(20));

                // Set scores and questions answered
                Map<Long, Integer> scores = new HashMap<>();
                Map<Long, Integer> questionsAnswered = new HashMap<>();
                Set<PlayerQuestionResult> questionResults = new HashSet<>();

                // Get session questions
                List<Question> sessionQuestions = getRandomSublist(allQuestions, 20);
                session.setQuestionIds(sessionQuestions.stream()
                        .map(Question::getId)
                        .collect(Collectors.toList()));

                for (Player sessionPlayer : sessionPlayers) {
                    scores.put(sessionPlayer.getId(), random.nextInt(10));
                    int answeredCount = random.nextInt(session.getCurrentQuestionIndex() + 1);
                    questionsAnswered.put(sessionPlayer.getId(), answeredCount);

                    // Create question results for each answered question
                    for (int q = 0; q < answeredCount; q++) {
                        PlayerQuestionResult result = new PlayerQuestionResult();
                        result.setPlayerId(sessionPlayer.getId());
                        result.setQuestionId(sessionQuestions.get(q).getId());
                        result.setQuestionIndex(q);
                        result.setCorrect(random.nextBoolean());
                        questionResults.add(result);
                    }
                }
                

                session.setScore(scores);
                session.setQuestionsAnswered(questionsAnswered);
                session.setQuestionResults(questionResults);
                session.setPlayedCategories(new HashSet<>());

                for (Long l : questionsAnswered.keySet()) {
                    if (questionsAnswered.get(l) < 18) {
                        session.setStatus(GameStatus.ACTIVE);
                    }
                } 
                if (!session.getStatus().equals(GameStatus.ACTIVE)) {
                    session.setStatus(GameStatus.COMPLETED);
                }

                sessionRepository.save(session);
            }
        }
    }

    private <T> List<T> getRandomSublist(List<T> list, int count) {
        return new ArrayList<>(list)
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        collected -> {
                            Collections.shuffle(collected);
                            return collected.stream()
                                    .limit(count)
                                    .collect(Collectors.toList());
                        }));
    }
}