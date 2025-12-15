package service.user.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import service.user.security.RateLimitingFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Test configuration to disable rate limiting during tests
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public RateLimitingFilter testRateLimitingFilter() {
        return new RateLimitingFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain)
                    throws ServletException, IOException {
                // Skip rate limiting in tests
                filterChain.doFilter(request, response);
            }
        };
    }
}

