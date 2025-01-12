package org.borg.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.borg.backend.auth.dto.*;
import org.borg.backend.auth.service.AuthService;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.DuplicateException;
import org.borg.backend.security.JwtFilter;
import org.borg.backend.security.JwtService;
import org.borg.backend.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void registerSuccess() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("testuser123")
                .password("password123")
                .displayName("Test User")
                .build();

        doNothing().when(authService).register(any(RegistrationRequest.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

    }

    @Test
    void registerUsernameTakenFailed() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("takenUsername")
                .password("password123")
                .displayName("Test User")
                .build();

        doThrow(new DuplicateException(BusinessErrorCodes.USERNAME_TAKEN, "Username is taken for: " + request.getUsername()))
                .when(authService).register(any(RegistrationRequest.class));


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.businessErrorCode").value(105))
                .andExpect(jsonPath("$.businessErrorDescription")
                        .value("Username is taken"))
                .andExpect(jsonPath("$.error").value("Username is taken for: " + request.getUsername()));
    }
    

    @Test
    void authenticateSuccess() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username("testuser123")
                .password("password123")
                .build();

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).authenticate(any(AuthRequest.class));
    }
    @Test
    void refreshTokenSuccess() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refreshToken")
                .build();

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(RefreshTokenResponse.builder().build());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).refresh(any(RefreshTokenRequest.class));
    }
}