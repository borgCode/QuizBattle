package org.borg.backend.seed;

import org.borg.backend.role.RoleRepository;
import org.borg.backend.player.PlayerRepository;
import org.borg.backend.singleplayer.model.Story;
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
        initStories();
    }

    private void initStories() {
        Story story = new Story();
        story.setTitle("The Dragon's Hoard");
        story.setDescription("A daring adventurer journeys across dangerous lands to steal a dragon’s legendary treasure. From treacherous forests to fiery mountains, every step is fraught with peril. But the greatest test awaits at the dragon's lair.");
        story.setIntroText("Legends speak of a dragon’s hoard, hidden deep within the heart of a distant land. Many have tried to claim it, none have returned. Now, a lone adventurer sets out to prove the myths true, braving the dangers of unknown realms for glory and wealth.");
        story.setImagePath("backend/src/main/java/org/borg/backend/storage/story/dragon_hoard/story_image.png");
        story.setNumOfChapters(7);
        
        
    }
}
