package api.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to add security headers to all gateway responses.
 * This addresses security concerns identified by ZAP scanning:
 * - Content Security Policy with frame-ancestors and form-action directives
 * - Additional security headers for defense-in-depth
 *
 * Uses .set() instead of .add() to prevent duplicate headers from backend services
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Add headers before the response is committed
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Content Security Policy - comprehensive policy including required directives
            // frame-ancestors: prevents clickjacking attacks
            // form-action: restricts where forms can be submitted
            headers.set("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
                "font-src 'self' data:; " +
                "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 http://localhost:9081; " +
                "frame-ancestors 'self'; " +
                "form-action 'self'; " +
                "base-uri 'self'; " +
                "object-src 'none';"
            );

            // X-Frame-Options - legacy protection against clickjacking (backup for older browsers)
            headers.set("X-Frame-Options", "SAMEORIGIN");

            // X-Content-Type-Options - prevents MIME type sniffing
            headers.set("X-Content-Type-Options", "nosniff");

            // X-XSS-Protection - enables XSS filter in older browsers
            headers.set("X-XSS-Protection", "1; mode=block");

            // Referrer-Policy - controls referrer information
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions-Policy - restricts browser features
            headers.set("Permissions-Policy",
                "geolocation=(), " +
                "microphone=(), " +
                "camera=(), " +
                "payment=(), " +
                "usb=(), " +
                "magnetometer=(), " +
                "gyroscope=(), " +
                "accelerometer=()"
            );

            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // High priority to ensure headers are added before other filters
        return -1;
    }
}

