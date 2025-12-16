package service.media.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import service.media.config.TestSecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUDIT TEST: Media Controller Tests
 * Tests to verify media upload and retrieval functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(TestSecurityConfig.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser", roles = {"SELLER"})
    void testUploadImage_WithValidFile_ShouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"SELLER"})
    void testUploadImage_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        mockMvc.perform(multipart("/api/media/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetImage_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/media/nonexistent-image.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllMedia_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/media"))
                .andExpect(status().isOk());
    }
    // public void testIntentionalFailure_ForAuditDemo() {
    //     org.junit.jupiter.api.Assertions.fail("This test is intentionally failing");
    // }
}

