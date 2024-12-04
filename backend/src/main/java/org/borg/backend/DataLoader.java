package org.borg.backend;

import org.borg.backend.role.Role;
import org.borg.backend.role.RoleRepository;
import org.borg.backend.player.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    private final PlayerRepository playerRepository;
    private final RoleRepository roleRepository;

    public DataLoader(PlayerRepository playerRepository, RoleRepository roleRepository) {
        this.playerRepository = playerRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
//        Question q1 = new Question("General Knowledge", "What is the capital of France?", Arrays.asList("Berlin", "Madrid", "Paris", "Rome"), "Paris");
//        Question q2 = new Question("Science", "What is the chemical symbol for water?", Arrays.asList("O2", "H2O", "CO2", "N2"), "H2O");
//        Question q3 = new Question("Math", "What is 2 + 2?", Arrays.asList("3", "4", "5", "6"), "4");
//        Question q4 = new Question("History", "Who was the first president of the USA?", Arrays.asList("George Washington", "Abraham Lincoln", "Thomas Jefferson", "John Adams"), "George Washington");
//        Question q5 = new Question("General Knowledge", "What is the largest ocean on Earth?", Arrays.asList("Atlantic Ocean", "Indian Ocean", "Pacific Ocean", "Arctic Ocean"), "Pacific Ocean");
//        Question q6 = new Question("Geography", "Which country has the most population?", Arrays.asList("India", "USA", "China", "Russia"), "China");
//        Question q7 = new Question("Technology", "What does HTML stand for?", Arrays.asList("Hyper Text Markup Language", "Hyper Tool Markup Language", "Home Tool Markup Language", "Hyper Transfer Markup Language"), "Hyper Text Markup Language");
//        Question q8 = new Question("Science", "What planet is known as the Red Planet?", Arrays.asList("Earth", "Mars", "Venus", "Jupiter"), "Mars");
//        Question q9 = new Question("Sports", "Which country won the 2018 FIFA World Cup?", Arrays.asList("Brazil", "France", "Germany", "Argentina"), "France");

//        Question q10 = new Question("General Knowledge", "Which element has the chemical symbol 'O'?", Arrays.asList("Gold", "Oxygen", "Osmium", "Olive"), "Oxygen");
//
//        
//        Question q13 = new Question("Science", "What is the boiling point of water at sea level?", Arrays.asList("50°C", "100°C", "150°C", "200°C"), "100°C");
//
//        Question q14 = new Question("Math", "What is the square root of 16?", Arrays.asList("2", "3", "4", "5"), "4");
//        Question q15 = new Question("Math", "If a triangle has angles of 90° and 45°, what is the measure of the third angle?", Arrays.asList("30°", "45°", "60°", "90°"), "45°");
//
//        Question q16 = new Question("History", "What year did World War II end?", Arrays.asList("1945", "1939", "1950", "1942"), "1945");
//        Question q17 = new Question("History", "Who discovered America in 1492?", Arrays.asList("Christopher Columbus", "Ferdinand Magellan", "Leif Erikson", "Amerigo Vespucci"), "Christopher Columbus");
//
//        Question q18 = new Question("Geography", "What is the largest desert in the world?", Arrays.asList("Sahara", "Antarctic Desert", "Arabian Desert", "Gobi Desert"), "Antarctic Desert");
//        Question q19 = new Question("Geography", "What is the capital of Japan?", Arrays.asList("Seoul", "Beijing", "Bangkok", "Tokyo"), "Tokyo");
//
//        Question q20 = new Question("Technology", "Who is known as the father of the computer?", Arrays.asList("Charles Babbage", "Alan Turing", "John von Neumann", "Thomas Edison"), "Charles Babbage");
//        Question q21 = new Question("Technology", "What is the most widely used programming language for Android app development?", Arrays.asList("Python", "Kotlin", "Java", "Swift"), "Java");
//
//        Question q22 = new Question("Sports", "How many players are there in a soccer team on the field?", Arrays.asList("9", "10", "11", "12"), "11");
//        Question q23 = new Question("Sports", "What is the national sport of Canada?", Arrays.asList("Ice Hockey", "Baseball", "Basketball", "Football"), "Ice Hockey");
//
////        
//        questionRepository.save(q10);
//        questionRepository.save(q13);
//        questionRepository.save(q14);
//        questionRepository.save(q15);
//        questionRepository.save(q16);
//        questionRepository.save(q17);
//        questionRepository.save(q18);
//        questionRepository.save(q19);
//        questionRepository.save(q20);
//
//        questionRepository.save(q21);
//        questionRepository.save(q22);
//        questionRepository.save(q23);
//
//        List<User> users = Arrays.asList(
//                new User(
//                        "dragonslayer92",
//                        "hashed_password_1",
//                        "Dragon Master",
//                        157,
//                        89,
//                        68
//                ),
//                new User(
//                        "pixelwarrior",
//                        "hashed_password_2",
//                        "Pixel Queen",
//                        243,
//                        132,
//                        111
//                ),
//                new User(
//                        "strategist_supreme",
//                        "hashed_password_3",
//                        "Master Tactician",
//                        89,
//                        45,
//                        44
//                ),
//                new User(
//                        "cosmic_gamer",
//                        "hashed_password_4",
//                        "Cosmic Chris",
//                        312,
//                        178,
//                        134
//                ),
//                new User(
//                        "neon_ninja",
//                        "hashed_password_5",
//                        "Neon Knight",
//                        195,
//                        98,
//                        97
//                )
//        );
//        for (User user : users) {
//            userRepository.save(user);
//        }
    }
}
