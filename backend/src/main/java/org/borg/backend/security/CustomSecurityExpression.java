package org.borg.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("customSecurityExpression")
public class CustomSecurityExpression {

    public boolean isPlayerOwner(Long requestedPlayerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) authentication.getCredentials();
            Long authenticatedPlayerId = (Long) details.get("playerId");
            return authenticatedPlayerId != null && authenticatedPlayerId.equals(requestedPlayerId);
        }
        return false;
    }
}
