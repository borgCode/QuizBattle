package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final InitDataService initDataService;
    private final PlayerRepository playerRepository;

    @Override
    public void run(String... args) {

//        initDataService.initRoles();
//        initDataService.initQuestions();
//        initDataService.initStoryData();
//        initDataService.initAchievements();

    }
}
