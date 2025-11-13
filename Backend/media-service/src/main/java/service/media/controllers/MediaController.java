package service.media.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.media.dtos.MediaDtos.*;
import service.media.services.MediaService;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "*")
public class MediaController {
    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;

    @Autowired
    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Upload a new media file (picture)
     * @param file The picture file to upload
     * @param uploaderId Optional uploader ID (for tracking who uploaded)
     * @return Upload response with file details
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploaderId", required = false, defaultValue = "anonymous") String uploaderId) {
        try {
            log.info("Uploading file: {}, size: {} bytes, uploader: {}",
                file.getOriginalFilename(), file.getSize(), uploaderId);

            MediaUploadResponse response = mediaService.uploadMedia(file, uploaderId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Get all uploaded media files
     * @return List of all media metadata
     */
    @GetMapping
    public ResponseEntity<List<MediaResponse>> getAllMedia() {
        try {
            List<MediaResponse> mediaList = mediaService.getAllMedia();
            log.info("Retrieved {} media files", mediaList.size());
            return ResponseEntity.ok(mediaList);
        } catch (Exception e) {
            log.error("Error retrieving media list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get media files by uploader ID
     * @param uploaderId The ID of the uploader
     * @return List of media uploaded by this user
     */
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<MediaResponse>> getMediaByUploaderId(@PathVariable String uploaderId) {
        try {
            List<MediaResponse> mediaList = mediaService.getMediaByUploaderId(uploaderId);
            log.info("Retrieved {} media files for uploader: {}", mediaList.size(), uploaderId);
            return ResponseEntity.ok(mediaList);
        } catch (Exception e) {
            log.error("Error retrieving media for uploader: {}", uploaderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get media metadata by ID
     * @param id The media ID
     * @return Media metadata
     */
    @GetMapping("/{id}/info")
    public ResponseEntity<?> getMediaInfo(@PathVariable String id) {
        try {
            MediaResponse media = mediaService.getMediaById(id);
            return ResponseEntity.ok(media);
        } catch (RuntimeException e) {
            log.warn("Media not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving media info for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download/view the actual media file
     * @param id The media ID
     * @return The file content
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMediaFile(@PathVariable String id) {
        try {
            MediaResponse mediaInfo = mediaService.getMediaById(id);
            byte[] fileContent = mediaService.getMediaFile(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mediaInfo.contentType()));
            headers.setContentLength(fileContent.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + mediaInfo.filename() + "\"");

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);

        } catch (RuntimeException e) {
            log.warn("Media file not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving media file for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a media file
     * @param id The media ID to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedia(@PathVariable String id) {
        try {
            mediaService.deleteMedia(id);
            log.info("Media deleted: {}", id);
            return ResponseEntity.ok(new MessageResponse("Media deleted successfully"));
        } catch (RuntimeException e) {
            log.warn("Media not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Failed to delete media"));
        }
    }
}

