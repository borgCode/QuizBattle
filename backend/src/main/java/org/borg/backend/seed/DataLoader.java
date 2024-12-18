package org.borg.backend.seed;

import lombok.RequiredArgsConstructor;
import org.borg.backend.question.Question;
import org.borg.backend.question.QuestionRepository;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.chapter.repository.ChapterRepository;
import org.borg.backend.story.repository.StoryRepository;
import org.borg.backend.chapter.model.Chapter;
import org.borg.backend.story.model.Story;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final InitDataService initDataService;


    @Override
    public void run(String... args) {

//        initDataService.initRoles();
//        initDataService.initQuestions();
//        initDataService.initStoryData();

    }
}
