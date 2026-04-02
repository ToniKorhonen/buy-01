package service.user.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User Service JWT authentication filter.
 * Extends the shared JwtAuthenticationFilter from shared-commons.
 * All filter logic is centralized to eliminate duplication across services.
 */
@Component
public class UserJwtAuthenticationFilter extends service.commons.security.JwtAuthenticationFilter {

    private final JwtService jwtService;

    @Autowired
    public UserJwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected String extractUserId(String token) {
        return jwtService.extractUserId(token);
    }

    @Override
    protected String extractRole(String token) {
        return jwtService.extractRole(token);
    }
}
