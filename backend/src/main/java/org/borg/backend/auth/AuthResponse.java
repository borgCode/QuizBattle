package org.borg.backend.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.PlayerDTO;

@Getter
@Setter
@Builder
public class AuthResponse {
    private String message;
    private PlayerDTO playerDTO;
    private String token;
}
