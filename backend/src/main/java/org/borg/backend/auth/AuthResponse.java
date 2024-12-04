package org.borg.backend.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.user.UserDTO;

@Getter
@Setter
@Builder
public class AuthResponse {
    private String message;
    private UserDTO userDTO;
    private String token;
}
