package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.question.Question;
import org.borg.backend.question.QuestionRepository;
import org.borg.backend.role.Role;
import org.borg.backend.role.RoleRepository;
import org.borg.backend.player.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final PlayerRepository playerRepository;
    private final RoleRepository roleRepository;
    private final SeedService seedService;
    private final QuestionRepository questionRepository;


    @Override
    public void run(String... args) {

//        List<Role> roles = createRoles();
//
//        readQuestions();

//        seedService.seedTestData();
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

    private void readQuestions() {

        try (BufferedReader reader = new BufferedReader(new FileReader("backend/src/main/java/org/borg/backend/seed/questionsData"))) {
            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(",", -1);

                if (parts.length == 4) {
                    String category = parts[0].trim();
                    String question = parts[1].trim();
                    String optionsLine = parts[2].trim();
                    List<String> options = Arrays.asList(optionsLine.split(";"));
                    String correctAnswer = parts[3].trim();

                    Question questionObj = new Question();
                    questionObj.setCategory(category);
                    questionObj.setQuestion(question);
                    questionObj.setOptions(options);
                    questionObj.setCorrectAnswer(correctAnswer);
                    questionRepository.save(questionObj);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
