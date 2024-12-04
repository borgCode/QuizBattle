package org.borg.backend.auth;

import lombok.RequiredArgsConstructor;
import org.borg.backend.role.Role;
import org.borg.backend.role.RoleRepository;
import org.borg.backend.security.JwtService;
import org.borg.backend.user.User;
import org.borg.backend.user.UserDTO;
import org.borg.backend.user.UserRepository;
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
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegistrationRequest request) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        User user = (User) auth.getPrincipal();
        String token = jwtService.generateToken(user);

        //Return userDTO if login is successful

        UserDTO userDTO = UserDTO.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .numOfGames(user.getNumOfGames())
                .numOfWins(user.getNumOfWins())
                .numOfLosses(user.getNumOfLosses())
                .build();

        return AuthResponse.builder()
                .message("Login successful")
                .userDTO(userDTO)
                .token(token)
                .build();

    }
}
