package org.borg.backend.auth.service;


import org.borg.backend.auth.dto.*;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.shared.exceptions.UserNameAlreadyTakenException;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AuthService authService;
    
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_DISPLAY_NAME = "Test User";

    private RegistrationRequest request;
    


    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();

        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
        request = RegistrationRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .displayName(TEST_DISPLAY_NAME)
                .build();
        
        authService.register(request);
    }

    @Test
    void registerNewUserSuccess() {
        assertTrue(playerRepository.findByUsername(TEST_USERNAME).isPresent());
    }
    
    @Test
    void registerDuplicateUserFailed() {
        assertThrows(UserNameAlreadyTakenException.class, () -> authService.register(request));
    }
    
    @Test
    void authenticateValidUserSuccess() {
        AuthRequest authRequest = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();

        AuthResponse authResponse = authService.authenticate(authRequest);
        
        assertNotNull(authResponse);
        assertNotNull(authResponse.getRefreshToken());
        assertNotNull(authResponse.getAccessToken());
        assertEquals("Login successful", authResponse.getMessage());
        assertEquals(TEST_USERNAME, authResponse.getPlayerDTO().getUsername());
        
    }
    
    @Test
    void authenticateWrongPasswordFail() {

        AuthRequest authRequest = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password("wrongpassword")
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(authRequest));
    }
    
    @Test
    void refreshTokenSuccess() {
        AuthRequest authRequest = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();

        AuthResponse authResponse = authService.authenticate(authRequest);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(authResponse.getRefreshToken())
                .build();
        RefreshTokenResponse refreshResponse = authService.refresh(refreshRequest);

        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getAccessToken());
    }
}
