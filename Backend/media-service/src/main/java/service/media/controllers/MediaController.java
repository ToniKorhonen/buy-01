package service.media.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import service.media.dtos.MediaDtos;
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
    public ResponseEntity<MediaDtos.MediaUploadResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) String productId) {
        log.info("Uploading file: {}, size: {} bytes, productId: {}",
            file.getOriginalFilename(), file.getSize(), productId);
        return ResponseEntity.ok(mediaService.uploadMedia(file, productId));
    }

    @GetMapping
    public List<MediaDtos.MediaResponse> getAllMedia() {
        return mediaService.getAllMedia();
    }

    @GetMapping("/product/{productId}")
    public List<MediaDtos.MediaResponse> getMediaByProductId(@PathVariable String productId) {
        return mediaService.getMediaByProductId(productId);
    }

    @GetMapping("/{id}/info")
    public MediaDtos.MediaResponse getMediaInfo(@PathVariable String id) {
        return mediaService.getMediaById(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getMediaFile(@PathVariable String id) {
        byte[] fileContent = mediaService.getMediaFile(id);

        String contentType = "application/octet-stream";
        if (fileContent.length > 4) {
            if (fileContent[0] == (byte) 0x89 && fileContent[1] == (byte) 0x50
                    && fileContent[2] == (byte) 0x4E && fileContent[3] == (byte) 0x47) {
                contentType = "image/png";
            } else if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xD8) {
                contentType = "image/jpeg";
            } else if (fileContent[0] == (byte) 0x47 && fileContent[1] == (byte) 0x49
                    && fileContent[2] == (byte) 0x46) {
                contentType = "image/gif";
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(fileContent.length);

        return ResponseEntity.ok().headers(headers).body(fileContent);
    }

    @PreAuthorize("hasRole('SELLER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MediaDtos.MessageResponse> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        log.info("Media deleted: {}", id);
        return ResponseEntity.ok(new MediaDtos.MessageResponse("Media deleted successfully"));
    }

    // Internal endpoint for product-service to delete all media when product is deleted
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<MediaDtos.MessageResponse> deleteAllMediaByProductId(@PathVariable String productId) {
        mediaService.deleteAllMediaByProductId(productId);
        log.info("All media deleted for product: {}", productId);
        return ResponseEntity.ok(new MediaDtos.MessageResponse("All media deleted successfully"));
    }
}
