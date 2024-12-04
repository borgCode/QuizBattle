package org.borg.backend;

import org.springframework.stereotype.Service;

@Service
public class SeedService {
    private final QuizGameSeeder seeder;

    public SeedService(QuizGameSeeder seeder) {
        this.seeder = seeder;
    }
    public void seedTestData() {
        seeder.seedDatabase(50, 100, 20); // 50 players, 100 questions, 20 sessions
    }
    
}
