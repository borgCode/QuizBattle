package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.game.shared.repository.QuestionRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final InitDataService initDataService;
    private final PlayerRepository playerRepository;
    private final InitFakeDataService initFakeDataService;
    private final RoleRepository roleRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

//        initDataService.initRoles();
//        initDataService.initQuestions();
//        initDataService.initStoryData();
//        initDataService.initAchievements();
//
//
//
//
//        //Faker 
//
//        generateFakerData();
//        log.info("Finished generating data");
 


    }

    

    private void generateFakerData() {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "USER role not initialized"));
        
        List<Player> players = playerRepository.saveAll(initFakeDataService.generateRandomPlayers(userRole, 100000));
        questionRepository.saveAll(initFakeDataService.generateRandomQuestions(100000));
        
        initFakeDataService.generateCompletedMultiplayerGames(players, 500000);
        initFakeDataService.generateActiveMultiplayerGames(players, 10000);
        initFakeDataService.generatePlayerStats(players);
    }
}
