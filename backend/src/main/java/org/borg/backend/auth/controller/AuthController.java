package org.borg.backend.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.auth.dto.*;
import org.borg.backend.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name="Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest request) {
        authService.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody @Valid AuthRequest authRequest) {
        return ResponseEntity.ok(authService.authenticate(authRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.warn("Refresh request received");
        return ResponseEntity.ok(authService.refresh(request));
    }
}
