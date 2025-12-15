package service.product.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUDIT TEST: Product Controller Tests
 * These tests verify the product endpoints work correctly
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllProducts_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateProduct_WithValidData_ShouldReturnCreated() throws Exception {
        String productJson = """
            {
                "name": "Test Product",
                "description": "A test product",
                "price": 99.99,
                "stock": 10,
                "category": "Electronics"
            }
        """;

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isCreated());
    }

    @Test
    void testCreateProduct_WithNegativePrice_ShouldReturnBadRequest() throws Exception {
        String invalidProductJson = """
            {
                "name": "Invalid Product",
                "description": "Invalid price",
                "price": -10.00,
                "stock": 10,
                "category": "Test"
            }
        """;

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_WithMissingName_ShouldReturnBadRequest() throws Exception {
        String invalidProductJson = """
            {
                "description": "Missing name",
                "price": 50.00,
                "stock": 5,
                "category": "Test"
            }
        """;

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetProductById_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/products/999999999999999999999999"))
                .andExpect(status().isNotFound());
    }

    /**
     * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
     * Uncomment to demonstrate test failure detection
     */
    // @Test
    // public void testIntentionalFailure_ForAuditDemo() {
    //     org.junit.jupiter.api.Assertions.fail("This test is intentionally failing");
    // }
}

