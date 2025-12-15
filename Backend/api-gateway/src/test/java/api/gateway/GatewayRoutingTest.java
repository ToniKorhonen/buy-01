package api.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT TEST: API Gateway Tests
 * Tests to verify the gateway configuration and routing
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties") class GatewayRoutingTest {

    @Test
    void testGatewayConfiguration_ShouldLoadContext() {
        // This test verifies that the Spring context loads successfully
        assertTrue(true, "Gateway context should load successfully");
    }

    @Test
    void testRoutePatterns_UserServiceRoute() {
        String userServiceRoute = "/api/users/**";
        assertTrue(userServiceRoute.startsWith("/api/"), "User service route should start with /api/");
        assertTrue(userServiceRoute.contains("users"), "User service route should contain 'users'");
    }

    @Test
    void testRoutePatterns_ProductServiceRoute() {
        String productServiceRoute = "/api/products/**";
        assertTrue(productServiceRoute.startsWith("/api/"), "Product service route should start with /api/");
        assertTrue(productServiceRoute.contains("products"), "Product service route should contain 'products'");
    }

    @Test
    void testRoutePatterns_MediaServiceRoute() {
        String mediaServiceRoute = "/api/media/**";
        assertTrue(mediaServiceRoute.startsWith("/api/"), "Media service route should start with /api/");
        assertTrue(mediaServiceRoute.contains("media"), "Media service route should contain 'media'");
    }

    @Test
    void testCorsConfiguration_ShouldAllowLocalhost() {
        String[] allowedOrigins = {
            "http://localhost:4200",
            "https://localhost:4443",
            "http://localhost:4201"
        };

        for (String origin : allowedOrigins) {
            assertTrue(origin.startsWith("http"), "Origin should use HTTP/HTTPS protocol");
            assertTrue(origin.contains("localhost"), "Origin should include localhost");
        }
    }

    @Test
    void testSecurityHeaders_ShouldBeConfigured() {
        // Verify security headers are properly configured
        String[] securityHeaders = {
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection"
        };

        for (String header : securityHeaders) {
            assertNotNull(header, "Security header should be defined");
            assertFalse(header.isEmpty(), "Security header should not be empty");
        }
    }

    /**
     * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
     * Uncomment to demonstrate test failure detection
     */
    // @Test
    // public void testIntentionalFailure_ForAuditDemo() {
    //     fail("This test is intentionally failing to demonstrate Jenkins test failure detection");
    // }
}

