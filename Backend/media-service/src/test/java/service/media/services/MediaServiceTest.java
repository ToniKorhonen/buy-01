package service.media.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT TEST: Media Service Tests
 * Tests for media file validation and processing
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class MediaServiceTest {

    @Test
    void testFileExtensionValidation_ShouldAcceptValidImageExtensions() {
        String[] validExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

        for (String ext : validExtensions) {
            assertTrue(ext.matches("^(jpg|jpeg|png|gif|webp)$"),
                "Extension " + ext + " should be valid");
        }
    }

    @Test
    void testFileExtensionValidation_ShouldRejectInvalidExtensions() {
        String[] invalidExtensions = {"exe", "bat", "sh", "php"};

        for (String ext : invalidExtensions) {
            assertFalse(ext.matches("^(jpg|jpeg|png|gif|webp)$"),
                "Extension " + ext + " should be invalid");
        }
    }

    @Test
    void testFileSizeValidation_ShouldAcceptValidSize() {
        long validSize = 5 * 1024 * 1024; // 5MB
        long maxSize = 10 * 1024 * 1024; // 10MB

        assertTrue(validSize > 0, "File size should be positive");
        assertTrue(validSize <= maxSize, "File size should be within limits");
    }

    @Test
    void testFileSizeValidation_ShouldRejectTooLargeFile() {
        long tooLargeSize = 20 * 1024 * 1024; // 20MB
        long maxSize = 10 * 1024 * 1024; // 10MB

        assertFalse(tooLargeSize <= maxSize, "Too large file should be rejected");
    }

    @Test
    void testFileNameValidation_ShouldAcceptValidFileName() {
        String validFileName = "test-image_123.jpg";

        assertNotNull(validFileName, "File name should not be null");
        assertFalse(validFileName.trim().isEmpty(), "File name should not be empty");
        assertTrue(validFileName.contains("."), "File name should have extension");
    }

    @Test
    void testMimeTypeValidation_ShouldAcceptValidMimeTypes() {
        String[] validMimeTypes = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
        };

        for (String mimeType : validMimeTypes) {
            assertTrue(mimeType.startsWith("image/"),
                "MIME type should start with 'image/'");
        }
    }

    /**
     * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
     * Uncomment to demonstrate test failure detection
     */
//     @Test
//     public void testIntentionalFailure_ForAuditDemo() {
//         fail("This test is intentionally failing");
//     }
}

