package org.borg.backend.unit.player;

import org.borg.backend.player.dto.ChangePasswordRequest;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.exceptions.PasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    
    
    @InjectMocks
    private PlayerService playerService;
    
    Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player = Player.builder()
                .id(1L)
                .username("player1")
                .password("oldPass")
                .build();
    }

    @Test
    void shouldChangePasswordWhenAllValidationsPass() {
        String currentPassword = "oldPass";
        String newPassword = "newPass";
        String encodedNewPassword = "encodedNewPass";

        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(passwordEncoder.matches(currentPassword, player.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .playerId(player.getId())
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmationPassword(newPassword).build();
        
        playerService.changePassword(request);
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        
        
        verify(playerRepository).save(captor.capture());
        assertEquals(encodedNewPassword, captor.getValue().getPassword());
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .playerId(player.getId())
                .currentPassword("wrongPass")
                .newPassword("newPass")
                .confirmationPassword("newPass").build();

        assertThrows(PasswordException.class, () -> playerService.changePassword(request));
        verify(playerRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewPasswordDoesNotMatchConfirmation() {
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .playerId(player.getId())
                .currentPassword("oldPass")
                .newPassword("newPass")
                .confirmationPassword("differentPass").build();

        assertThrows(PasswordException.class, () -> playerService.changePassword(request));
        verify(playerRepository, never()).save(any());
    }
}
