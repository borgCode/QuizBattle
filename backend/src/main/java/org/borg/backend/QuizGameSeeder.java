package org.borg.backend;

import com.github.javafaker.Faker;
import org.borg.backend.multiplayer.GameStatus;
import org.borg.backend.multiplayer.MultiplayerSession;
import org.borg.backend.multiplayer.MultiplayerSessionRepository;
import org.borg.backend.player.Player;
import org.borg.backend.player.PlayerRepository;
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
            MultiplayerSessionRepository sessionRepository, PasswordEncoder passwordEncoder) {
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
        // Create roles first
        List<Role> roles = createRoles();

        // Create players
        List<Player> players = createPlayers(numPlayers, roles);

        // Create questions
        List<Question> questions = createQuestions(numQuestions);

        // Create multiplayer sessions
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

    private List<Player> createPlayers(int count, List<Role> roles) {

        
        
        Role userRole = roles.stream()
                .filter(r -> r.getName().equals("USER"))
                .findFirst()
                .orElseThrow();

        List<Player> players = new ArrayList<>();
        Player testUser = new Player();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password")); // password is "password"
        testUser.setDisplayName("Test User");
        testUser.setNumOfGames(10);
        testUser.setNumOfWins(5);
        testUser.setNumOfLosses(5);
        testUser.setAccountLocked(false);
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singletonList(userRole));
        players.add(playerRepository.save(testUser));
        
        for (int i = 0; i < count; i++) {
            Player player = new Player();
            player.setUsername(faker.name().username() + random.nextInt(1000));
            player.setPassword("$2a$10$" + faker.crypto().sha256().substring(0, 50));
            player.setDisplayName(faker.superhero().name());
            player.setNumOfGames(random.nextInt(50));
            if (player.getNumOfGames() > 0) {
                player.setNumOfWins(random.nextInt(player.getNumOfGames()));
            } else {
                player.setNumOfWins(0); 
            }
            player.setNumOfLosses(player.getNumOfGames() - player.getNumOfWins());
            player.setAccountLocked(false);
            player.setEnabled(true);
            player.setRoles(Collections.singletonList(userRole));

            players.add(playerRepository.save(player));
        }
        return players;
    }

    private List<Question> createQuestions(int count) {
        List<String> categories = Arrays.asList("History", "Science", "Geography", "Entertainment", "Sports");
        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Question question = new Question();
            question.setCategory(categories.get(random.nextInt(categories.size())));
            question.setQuestion(faker.lorem().sentence() + "?");

            // Generate 4 options with one being correct
            List<String> options = new ArrayList<>();
            String correctAnswer = faker.lorem().word();
            options.add(correctAnswer);

            for (int j = 0; j < 3; j++) {
                options.add(faker.lorem().word());
            }

            // Shuffle options
            Collections.shuffle(options);
            question.setOptions(options);
            question.setCorrectAnswer(correctAnswer);

            questions.add(questionRepository.save(question));
        }

        return questions;
    }

    private void createMultiplayerSessions(int count, List<Player> allPlayers, List<Question> allQuestions) {
        for (int i = 0; i < count; i++) {
            MultiplayerSession session = new MultiplayerSession();

            // Add 2-4 random players to session
            int numPlayersInSession = random.nextInt(1) + 2;
            List<Player> sessionPlayers = getRandomSublist(allPlayers, numPlayersInSession);
            session.setPlayers(sessionPlayers);

            // Set random current player
            session.setCurrentPlayerTurn(sessionPlayers.get(random.nextInt(sessionPlayers.size())));

            // Set random game status
            session.setStatus(GameStatus.values()[random.nextInt(GameStatus.values().length)]);

            // Set current question index
            session.setCurrentQuestionIndex(random.nextInt(20));

            // Initialize scores
            Map<Long, Integer> scores = sessionPlayers.stream()
                    .collect(Collectors.toMap(
                            Player::getId,
                            player -> random.nextInt(10)
                    ));
            session.setScore(scores);

            // Initialize questions answered
            Map<Long, Integer> questionsAnswered = sessionPlayers.stream()
                    .collect(Collectors.toMap(
                            Player::getId,
                            player -> random.nextInt(session.getCurrentQuestionIndex() + 1)
                    ));
            session.setQuestionsAnswered(questionsAnswered);

            // Add random questions
            List<Long> questionIds = getRandomSublist(allQuestions, 20).stream()
                    .map(Question::getId)
                    .collect(Collectors.toList());
            session.setQuestionIds(questionIds);

            sessionRepository.save(session);
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
