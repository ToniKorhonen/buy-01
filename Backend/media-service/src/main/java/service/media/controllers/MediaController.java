package service.media.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.media.dtos.MediaDtos;
import service.media.exception.AccessDeniedException;
import service.media.services.MediaService;

import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;

    @Autowired
    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) String productId,
            Authentication auth) {
        try {
            checkSellerRole(auth);
            log.info("Uploading file: {}, size: {} bytes, productId: {}",
                file.getOriginalFilename(), file.getSize(), productId);
            MediaDtos.MediaUploadResponse response = mediaService.uploadMedia(file, productId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MediaDtos.MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MediaDtos.MessageResponse("Failed to upload file: " + e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<List<MediaDtos.MediaResponse>> getAllMedia() {
        try {
            List<MediaDtos.MediaResponse> mediaList = mediaService.getAllMedia();
            log.info("Retrieved {} media files", mediaList.size());
            return ResponseEntity.ok(mediaList);
        } catch (Exception e) {
            log.error("Error retrieving media list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<MediaDtos.MediaResponse>> getMediaByProductId(@PathVariable String productId) {
        try {
            List<MediaDtos.MediaResponse> mediaList = mediaService.getMediaByProductId(productId);
            log.info("Retrieved {} media files for product: {}", mediaList.size(), productId);
            return ResponseEntity.ok(mediaList);
        } catch (Exception e) {
            log.error("Error retrieving media for product: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}/info")
    public ResponseEntity<?> getMediaInfo(@PathVariable String id) {
        try {
            MediaDtos.MediaResponse media = mediaService.getMediaById(id);
            return ResponseEntity.ok(media);
        } catch (RuntimeException e) {
            log.warn("Media not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving media info for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getMediaFile(@PathVariable String id) {
        try {
            byte[] fileContent = mediaService.getMediaFile(id);

            String contentType = "application/octet-stream";

            if (fileContent.length > 4) {
                if (fileContent[0] == (byte)0x89 && fileContent[1] == (byte)0x50 &&
                    fileContent[2] == (byte)0x4E && fileContent[3] == (byte)0x47) {
                    contentType = "image/png";
                } else if (fileContent[0] == (byte)0xFF && fileContent[1] == (byte)0xD8) {
                    contentType = "image/jpeg";
                } else if (fileContent[0] == (byte)0x47 && fileContent[1] == (byte)0x49 &&
                          fileContent[2] == (byte)0x46) {
                    contentType = "image/gif";
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileContent.length);

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


    @PreAuthorize("hasRole('SELLER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedia(@PathVariable String id, Authentication auth) {
        try {
            checkSellerRole(auth);
            mediaService.deleteMedia(id);
            log.info("Media deleted: {}", id);
            return ResponseEntity.ok(new MediaDtos.MessageResponse("Media deleted successfully"));
        } catch (RuntimeException e) {
            log.warn("Media not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MediaDtos.MessageResponse("Failed to delete media"));
        }
    }

    // Internal endpoint for product-service to delete all media when product is deleted
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> deleteAllMediaByProductId(@PathVariable String productId) {
        try {
            mediaService.deleteAllMediaByProductId(productId);
            log.info("All media deleted for product: {}", productId);
            return ResponseEntity.ok(new MediaDtos.MessageResponse("All media deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting media for product: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MediaDtos.MessageResponse("Failed to delete media"));
        }
    }

    private void checkSellerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new AccessDeniedException("Only sellers can upload or delete media");
        }

        boolean isSeller = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_SELLER"));

        if (!isSeller) {
            throw new AccessDeniedException("Only sellers can upload or delete media");
        }
    }
}

