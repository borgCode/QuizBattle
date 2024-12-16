package org.borg.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.dto.AuthRequest;
import org.borg.backend.auth.dto.AuthResponse;
import org.borg.backend.auth.dto.RegistrationRequest;
import org.borg.backend.common.enums.BusinessErrorCodes;
import org.borg.backend.common.exceptions.UserNameAlreadyTakenException;
import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.security.JwtService;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.repository.PlayerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
        
        try {
            playerRepository.save(player);
        } catch (DataIntegrityViolationException e) {
            throw new UserNameAlreadyTakenException(BusinessErrorCodes.USERNAME_TAKEN);
        }
       
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
