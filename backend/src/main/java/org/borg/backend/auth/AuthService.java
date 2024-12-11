package org.borg.backend.auth;

import lombok.RequiredArgsConstructor;
import org.borg.backend.role.Role;
import org.borg.backend.role.RoleRepository;
import org.borg.backend.security.JwtService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.PlayerMapper;
import org.borg.backend.player.PlayerRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerRepository playerRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegistrationRequest request) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));
        Player player = Player.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();
        playerRepository.save(player);
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        Player player = (Player) auth.getPrincipal();
        String token = jwtService.generateToken(player);

        //Return userDTO if login is successful

        PlayerDTO playerDTO = PlayerMapper.toDTO(player);

        return AuthResponse.builder()
                .message("Login successful")
                .playerDTO(playerDTO)
                .token(token)
                .build();

    }
}
