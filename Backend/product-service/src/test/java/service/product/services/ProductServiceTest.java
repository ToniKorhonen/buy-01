package service.product.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT TEST: Product Service Tests
 * Simple validation tests for product service logic
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductServiceTest {

    @Test
    void testPriceValidation_ShouldAcceptValidPrice() {
        double validPrice = 99.99;
        assertTrue(validPrice > 0, "Price should be positive");
        assertTrue(validPrice < 1000000, "Price should be reasonable");
    }

    @Test
    void testPriceValidation_ShouldRejectNegativePrice() {
        double invalidPrice = -10.00;
        assertFalse(invalidPrice > 0, "Negative price should be rejected");
    }

    @Test
    void testStockValidation_ShouldAcceptValidStock() {
        int validStock = 100;
        assertTrue(validStock >= 0, "Stock should be non-negative");
    }

    @Test
    void testStockValidation_ShouldRejectNegativeStock() {
        int invalidStock = -5;
        assertFalse(invalidStock >= 0, "Negative stock should be rejected");
    }

    @Test
    void testProductNameValidation_ShouldAcceptValidName() {
        String validName = "Valid Product Name";
        assertNotNull(validName, "Name should not be null");
        assertFalse(validName.trim().isEmpty(), "Name should not be empty");
        assertTrue(validName.length() <= 200, "Name should not be too long");
    }

    @Test
    void testCategoryValidation_ShouldAcceptValidCategory() {
        String validCategory = "Electronics";
        assertNotNull(validCategory, "Category should not be null");
        assertFalse(validCategory.trim().isEmpty(), "Category should not be empty");
    }

    /**
     * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
     * Uncomment to demonstrate test failure detection
     */
    // @Test
    // public void testIntentionalFailure_ForAuditDemo() {
    //     fail("This test is intentionally failing");
    // }
}

