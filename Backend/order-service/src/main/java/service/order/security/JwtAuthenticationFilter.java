package service.order.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Order Service JWT authentication filter.
 * Extends the shared JwtAuthenticationFilter from shared-commons.
 */
@Component
public class JwtAuthenticationFilter extends service.commons.security.JwtAuthenticationFilter {

    private final JwtService jwtService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService) {
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

