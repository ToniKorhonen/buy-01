package service.user.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * AUDIT TEST: Authentication Controller Tests
 * These tests verify the authentication endpoints work correctly
 * and fail the build if authentication is broken.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
     void testRegisterWithValidData_ShouldReturnCreated() throws Exception {
        String validUserJson = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUserJson))
                .andExpect(status().isCreated());
    }

    @Test
    void testRegisterWithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        String invalidUserJson = """
            {
                "username": "testuser",
                "email": "not-an-email",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterWithMissingUsername_ShouldReturnBadRequest() throws Exception {
        String invalidUserJson = """
            {
                "email": "test@example.com",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginWithValidCredentials_ShouldReturnOk() throws Exception {
        // First register a user
        String registerJson = """
            {
                "username": "logintest",
                "email": "logintest@example.com",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson));

        // Then try to login
        String loginJson = """
            {
                "email": "logintest@example.com",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testLoginWithInvalidPassword_ShouldReturnUnauthorized() throws Exception {
        // First register a user
        String registerJson = """
            {
                "username": "passwordtest",
                "email": "passwordtest@example.com",
                "password": "Password123!"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson));

        // Try to login with wrong password
        String loginJson = """
            {
                "email": "passwordtest@example.com",
                "password": "WrongPassword!"
            }
        """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    /**
     * INTENTIONALLY BREAKABLE TEST FOR AUDIT DEMO
     * To demonstrate pipeline failure on test failure,
     * you can uncomment this test and it will fail the build.
     */
    // @Test
    // public void testIntentionalFailure_ForAuditDemo() {
    //     org.junit.jupiter.api.Assertions.fail("This test is intentionally failing to demonstrate Jenkins test failure detection");
    // }
}

