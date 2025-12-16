package service.media;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for Media Service Application.
 * This test class ensures the application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class MediaServiceApplicationTests {

    /**
     * Test that the Spring application context loads without errors.
     * This is a basic smoke test to verify the application configuration.
     */
    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }

}

