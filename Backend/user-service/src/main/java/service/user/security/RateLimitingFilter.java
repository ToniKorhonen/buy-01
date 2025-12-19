package service.user.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Rate limiting filter to prevent brute force and spam attacks.
 * Applies different limits based on endpoint type.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    // Configuration from application.properties
    @Value("${rate.limit.login.max-per-minute:5}")
    private int maxLoginAttempts;
    @Value("${rate.limit.register.max-per-minute:3}")
    private int maxRegisterAttempts;
    @Value("${rate.limit.window-ms:60000}")
    private long timeWindowMs;
    // Separate storage for different endpoint types
    private final ConcurrentHashMap<String, RateLimitData> loginRateLimitMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitData> registerRateLimitMap = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        boolean shouldContinue = true;

        // Apply rate limiting based on endpoint
        if (uri.contains("/auth/login")) {
            shouldContinue = checkRateLimit(clientIp, loginRateLimitMap, maxLoginAttempts, "login", response);
        } else if (uri.contains("/auth/register")) {
            shouldContinue = checkRateLimit(clientIp, registerRateLimitMap, maxRegisterAttempts, "register", response);
        }

        if (!shouldContinue) {
            return;
        }

        // Clean up old entries periodically
        cleanupOldEntries();
        filterChain.doFilter(request, response);
    }
    /**
     * Check if request is within rate limit.
     * Returns false if limit exceeded (and sends 429 response).
     */
    private boolean checkRateLimit(String clientIp, ConcurrentHashMap<String, RateLimitData> rateLimitMap, 
                                   int maxRequests, String endpointType, HttpServletResponse response) throws IOException {
        RateLimitData rateLimitData = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitData());
        synchronized (rateLimitData) {
            long currentTime = System.currentTimeMillis();
            // Reset counter if time window has passed
            if (currentTime - rateLimitData.windowStart > timeWindowMs) {
                rateLimitData.requestCount.set(0);
                rateLimitData.windowStart = currentTime;
            }
            // Check if limit exceeded
            if (rateLimitData.requestCount.get() >= maxRequests) {
                log.warn("Rate limit exceeded for {} from IP: {}, attempts: {}", 
                    endpointType, clientIp, rateLimitData.requestCount.get());
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                    "{\"error\":\"Too many %s attempts. Maximum %d requests per minute. Please try again later.\"}",
                    endpointType, maxRequests
                ));
                return false;
            }
            // Increment counter
            rateLimitData.requestCount.incrementAndGet();
            log.debug("{} request allowed for IP: {}, count: {}/{}", 
                endpointType, clientIp, rateLimitData.requestCount.get(), maxRequests);
        }
        return true;
    }
    /**
     * Get the client IP address from the request.
     * Checks X-Forwarded-For header for proxy scenarios.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    /**
     * Clean up old entries to prevent memory leaks.
     * Removes entries older than 2 time windows.
     */
    private void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = timeWindowMs * 2;
        loginRateLimitMap.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().windowStart > cleanupThreshold
        );
        registerRateLimitMap.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().windowStart > cleanupThreshold
        );
    }
    /**
     * Data class to store rate limiting information per IP.
     */
    private static class RateLimitData {
        AtomicInteger requestCount = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }
}
