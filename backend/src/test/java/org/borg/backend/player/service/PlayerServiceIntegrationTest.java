package org.borg.backend.player.service;

import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.dto.RegistrationRequest;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.auth.service.AuthService;
import org.borg.backend.player.dto.ChangePasswordRequest;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.PasswordException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    
    Player player;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @Nested
    class PasswordChangeTests {
        private static final String TEST_USERNAME = "testuser";
        private static final String CURRENT_PASSWORD = "password123";
        private static final String NEW_PASSWORD = "password321";
        private static final String TEST_DISPLAY_NAME = "Test User";

        @BeforeEach
        void setUp() {
            authService.register(RegistrationRequest.builder()
                    .username(TEST_USERNAME)
                    .password(CURRENT_PASSWORD)
                    .displayName(TEST_DISPLAY_NAME)
                    .build());

            player = playerRepository.findByUsername(TEST_USERNAME)
                    .orElseThrow();
        }

        @AfterEach
        void tearDown() {
            playerRepository.deleteAll();
        }

        @Test
        void changePasswordSuccess() {
            playerService.changePassword(ChangePasswordRequest.builder()
                    .currentPassword(CURRENT_PASSWORD)
                    .newPassword(NEW_PASSWORD)
                    .confirmationPassword(NEW_PASSWORD)
                    .playerId(player.getId())
                    .build());

            Player updatedPlayer = playerService.getPlayerById(player.getId());
            assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedPlayer.getPassword()));
        }

        @Test
        void shouldThrowError_WhenWrongCurrentPassword() {
            PasswordException exception = assertThrows(PasswordException.class,
                    () -> playerService.changePassword(ChangePasswordRequest.builder()
                            .currentPassword("wrong password")
                            .newPassword(NEW_PASSWORD)
                            .confirmationPassword(NEW_PASSWORD)
                            .playerId(player.getId())
                            .build()));

            assertEquals(BusinessErrorCodes.INCORRECT_CURRENT_PASSWORD, exception.getErrorCode());
        }

        @Test
        void shouldThrowError_WhenPasswordsNotMatch() {
            PasswordException exception = assertThrows(PasswordException.class,
                    () -> playerService.changePassword(ChangePasswordRequest.builder()
                            .currentPassword(CURRENT_PASSWORD)
                            .newPassword("different password")
                            .confirmationPassword(NEW_PASSWORD)
                            .playerId(player.getId())
                            .build()));

            assertEquals(BusinessErrorCodes.NEW_PASSWORD_DOES_NOT_MATCH, exception.getErrorCode());
        }
    }

    @Nested
    class ProfilePictureTests {

        @BeforeEach
        void setUp() {
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

        @Value("${file.path.storage}")
        private String testStoragePath;

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
}