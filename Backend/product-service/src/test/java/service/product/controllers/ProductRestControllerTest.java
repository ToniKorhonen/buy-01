package service.product.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import service.product.mongo_repo.ProductRepository;

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

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Clean the database before each test to ensure test isolation
        productRepository.deleteAll();
    }

    @Test
    void testGetAllProducts_ShouldReturnOk() throws Exception {
        // GET / is public - no authentication needed
        // Should return OK even if empty list
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"SELLER"})
    void testCreateProduct_WithValidData_ShouldReturnCreated() throws Exception {
        String productJson = """
            {
                "name": "Test Product",
                "description": "A test product",
                "price": 99.99,
                "quantity": 10
            }
        """;

        // POST / requires authentication and SELLER role
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"SELLER"})
    void testCreateProduct_WithNegativePrice_ShouldReturnBadRequest() throws Exception {
        String invalidProductJson = """
            {
                "name": "Invalid Product",
                "description": "Invalid price",
                "price": -10.00,
                "quantity": 10
            }
        """;

        // POST / requires authentication and SELLER role
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"SELLER"})
    void testCreateProduct_WithMissingName_ShouldReturnBadRequest() throws Exception {
        String invalidProductJson = """
            {
                "description": "Missing name",
                "price": 50.00,
                "quantity": 5
            }
        """;

        // POST / requires authentication and SELLER role
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetProductById_NonExistent_ShouldReturnNotFound() throws Exception {
        // GET /{id} is public - no authentication needed
        mockMvc.perform(get("/999999999999999999999999"))
                .andExpect(status().isNotFound());
    }
}

