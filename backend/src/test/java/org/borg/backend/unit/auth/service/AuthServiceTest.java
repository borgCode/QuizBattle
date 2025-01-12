package org.borg.backend.unit.auth.service;

import org.borg.backend.auth.dto.*;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.auth.service.AuthService;
import org.borg.backend.shared.exceptions.DuplicateException;
import org.borg.backend.shared.util.ImageUtil;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.Stats;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AuthServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;


    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void registerWithValidDataThenSucceed() {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("testuser123")
                .password("password123")
                .displayName("Test User").build();

        Role userRole = new Role();
        userRole.setName("USER");

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword123");

        Player expectedPlayer = Player.builder()
                .username("testuser123")
                .password("encodedPassword123")
                .displayName("Test User")
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();
        when(playerRepository.save(any(Player.class)))
                .thenReturn(expectedPlayer);

        authService.register(request);

        verify(roleRepository).findByName("USER");
        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(playerCaptor.capture());

        Player capturedPlayer = playerCaptor.getValue();
        assertEquals("testuser123", capturedPlayer.getUsername());
        assertEquals("encodedPassword123", capturedPlayer.getPassword());
        assertEquals("Test User", capturedPlayer.getDisplayName());
        assertFalse(capturedPlayer.isAccountLocked());
        assertTrue(capturedPlayer.isEnabled());
        assertEquals(List.of(userRole), capturedPlayer.getRoles());

    }

    @Test
    void registerWithExistingUsernameThenThrowException() {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("existinguser")
                .password("password123")
                .displayName("Test User")
                .build();

        Role userRole = new Role();
        userRole.setName("USER");

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");
        when(playerRepository.save(any(Player.class)))
                .thenThrow(new DataIntegrityViolationException("Username already exists"));

        assertThrows(DuplicateException.class, () ->
                authService.register(request)
        );
    }

    @Test
    void authenticate() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class);
             MockedStatic<PlayerMapper> mapperMock = Mockito.mockStatic(PlayerMapper.class)) {
            AuthRequest authRequest = AuthRequest.builder()
                    .username("testuser123")
                    .password("password123")
                    .build();
            
            Authentication auth = Mockito.mock(Authentication.class);

            when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(),
                    authRequest.getPassword())
            )).thenReturn(auth);

            Player mockPlayer = Player.builder()
                    .username("testuser123")
                    .displayName("Test User")
                    .avatarPath("/path/to/avatar.jpg")
                    .stats(new Stats())
                    .build();

            when(auth.getPrincipal()).thenReturn(mockPlayer);

            String accessToken = "mockAccessToken";
            String refreshToken = "mockRefreshToken";

            when(jwtService.generateToken(mockPlayer)).thenReturn(accessToken);
            when(jwtService.generateRefreshToken(mockPlayer)).thenReturn(refreshToken);

            imageUtilMock.when(() -> ImageUtil.encodeAvatarImageFileToBase64("/path/to/avatar.jpg"))
                    .thenReturn("base64Image");

            PlayerDTO mockPlayerDTO = PlayerDTO.builder()
                    .id(1L)
                    .username("testuser123")
                    .displayName("Test User")
                    .stats(new Stats())
                    .base64Image("base64Image").
                    build();

            mapperMock.when(() -> PlayerMapper.toDTO(mockPlayer))
                    .thenReturn(mockPlayerDTO);

            AuthResponse authResponse = authService.authenticate(authRequest);

            assertEquals("Login successful", authResponse.getMessage());
            assertEquals(mockPlayerDTO, authResponse.getPlayerDTO());
            assertEquals(accessToken, authResponse.getAccessToken());
            assertEquals(refreshToken, authResponse.getRefreshToken());

        }
    }
    
    @Test
    void refreshTokenSuccess() {

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refreshToken").
                build();
        
        when(jwtService.createNewAccessToken(request.getRefreshToken())).thenReturn("newAccessToken");

        RefreshTokenResponse response = authService.refresh(request);
        
        assertEquals("newAccessToken", response.getAccessToken());
        verify(jwtService).createNewAccessToken("refreshToken");
    }
}