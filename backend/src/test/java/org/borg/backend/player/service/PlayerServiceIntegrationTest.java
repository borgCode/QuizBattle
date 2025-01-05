package org.borg.backend.player.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class PlayerServiceIntegrationTest {
    
    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${file.path.storage}")
    private String testStoragePath;
    
    Player player;
    @Autowired
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        player = Player.builder()
                .username("testPlayer")
                .password("password")
                .displayName("Test Player ")
                .accountLocked(false)
                .enabled(true)
                .roles(new ArrayList<>(List.of(userRole)))
                .build();

        playerRepository.save(player);
    }
    

    @AfterEach
    void tearDown() {
        File testDir = new File(testStoragePath);
        if (testDir.exists()) {
            deleteDirectory(testDir);
        }
    }

    private void deleteDirectory(File testDir) {
        File[] files = testDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        testDir.delete();
    }

    @Test
    void uploadProfilePictureSuccess() {
        String fileName = "test-profile.jpg";
        String contentType = "image/jpeg";
        byte[] content = "test image content".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile("file", fileName, contentType, content);
        
        playerService.uploadProfilePicture(player.getId(), mockFile);
        
        Player updatedPlayer = playerRepository.findById(player.getId())
                .orElseThrow();
        
        String avatarPath = updatedPlayer.getAvatarPath();
        assertTrue(avatarPath.contains(player.getId() + File.separator), "Avatar path should include playerId and a separator");
       
        
        String expectedPathName = testStoragePath + File.separator + "profile-pics" + File.separator + avatarPath;
        File savedFile = new File(expectedPathName);
        assertTrue(savedFile.exists(), "Profile picture file should exist");
        
        String savedFileName = savedFile.getName();
        assertTrue(savedFileName.matches("\\d+\\.jpg"), "Filename should have timestamp pattern");
        
        
    }
}