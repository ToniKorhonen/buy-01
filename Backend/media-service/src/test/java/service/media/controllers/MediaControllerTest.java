package service.media.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUDIT TEST: Media Controller Tests
 * Tests to verify media upload and retrieval functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUploadImage_WithValidFile_ShouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMvc.perform(multipart("/media/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void testUploadImage_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        mockMvc.perform(multipart("/media/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetImage_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/media/nonexistent-image.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllMedia_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/media"))
                .andExpect(status().isOk());
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

