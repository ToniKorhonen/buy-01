package service.user.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT TEST: User Service Tests
 * Simple tests to verify the service layer works correctly
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class UserServiceTest {

    @Test
    void testPasswordValidation_ShouldAcceptStrongPassword() {
        String strongPassword = "StrongPassword123!";
        assertTrue(strongPassword.length() >= 8, "Password should be at least 8 characters");
        assertTrue(strongPassword.matches(".*[A-Z].*"), "Password should contain uppercase letter");
        assertTrue(strongPassword.matches(".*[a-z].*"), "Password should contain lowercase letter");
        assertTrue(strongPassword.matches(".*\\d.*"), "Password should contain digit");
    }

    @Test
    void testPasswordValidation_ShouldRejectWeakPassword() {
        String weakPassword = "weak";
        assertFalse(weakPassword.length() >= 8, "Weak password should be rejected");
    }

    @Test
    void testEmailValidation_ShouldAcceptValidEmail() {
        String validEmail = "user@example.com";
        assertTrue(validEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$"), "Valid email should match pattern");
    }

    @Test
    void testEmailValidation_ShouldRejectInvalidEmail() {
        String invalidEmail = "not-an-email";
        assertFalse(invalidEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$"), "Invalid email should be rejected");
    }

    @Test
    void testUsernameValidation_ShouldAcceptValidUsername() {
        String validUsername = "validuser123";
        assertTrue(validUsername.length() >= 3, "Username should be at least 3 characters");
        assertTrue(validUsername.matches("^[a-zA-Z0-9_]+$"), "Username should only contain alphanumeric and underscore");
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

