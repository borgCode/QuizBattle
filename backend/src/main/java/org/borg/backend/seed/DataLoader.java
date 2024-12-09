package org.borg.backend.seed;

import org.borg.backend.role.RoleRepository;
import org.borg.backend.player.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    private final PlayerRepository playerRepository;
    private final RoleRepository roleRepository;
    private final SeedService seedService;
    
    

    public DataLoader(PlayerRepository playerRepository, RoleRepository roleRepository, SeedService seedService) {
        this.playerRepository = playerRepository;
        this.roleRepository = roleRepository;
        this.seedService = seedService;
    }

    @Override
    public void run(String... args) {
        
//        seedService.seedTestData();
    }
}
