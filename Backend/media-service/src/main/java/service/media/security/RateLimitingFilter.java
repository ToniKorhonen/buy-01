package service.media.security;
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
 * Rate limiting filter to prevent abuse of upload endpoints.
 * Limits requests per IP address per time window.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    // Configuration from application.properties
    @Value("${rate.limit.uploads.max-per-minute:10}")
    private int maxRequestsPerMinute;
    @Value("${rate.limit.uploads.window-ms:60000}")
    private long timeWindowMs;
    // Storage for rate limiting data
    private final ConcurrentHashMap<String, RateLimitData> rateLimitMap = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Only apply rate limiting to upload endpoint
        if (!request.getRequestURI().contains("/api/media/upload")) {
            filterChain.doFilter(request, response);
            return;
        }
        String clientIp = getClientIp(request);
        // Clean up old entries periodically
        cleanupOldEntries();
        // Get or create rate limit data for this IP
        RateLimitData rateLimitData = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitData());
        synchronized (rateLimitData) {
            long currentTime = System.currentTimeMillis();
            // Reset counter if time window has passed
            if (currentTime - rateLimitData.windowStart > timeWindowMs) {
                rateLimitData.requestCount.set(0);
                rateLimitData.windowStart = currentTime;
            }
            // Check if limit exceeded
            if (rateLimitData.requestCount.get() >= maxRequestsPerMinute) {
                log.warn("Rate limit exceeded for IP: {}, requests: {}", clientIp, rateLimitData.requestCount.get());
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Maximum " + maxRequestsPerMinute + " uploads per minute.\"}");
                return;
            }
            // Increment counter
            rateLimitData.requestCount.incrementAndGet();
            log.debug("Request allowed for IP: {}, count: {}/{}", clientIp, rateLimitData.requestCount.get(), maxRequestsPerMinute);
        }
        filterChain.doFilter(request, response);
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
        rateLimitMap.entrySet().removeIf(entry -> 
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
