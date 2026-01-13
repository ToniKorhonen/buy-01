package service.media.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import service.media.dtos.MediaDtos.*;
import service.media.exception.MediaNotFoundException;
import service.media.exception.StorageException;
import service.media.models.Media;
import service.media.mongo_repo.MediaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final String MEDIA_NOT_FOUND_MESSAGE = "Media not found with id: ";

    private final MediaRepository mediaRepository;
    private final Path storageLocation;
    private final List<String> allowedTypes;
    private final String baseUrl;

    @Autowired
    public MediaService(MediaRepository mediaRepository,
                       @Value("${media.storage.path:./uploads}") String storagePath,
                       @Value("${media.allowed.types:image/png,image/jpeg,image/gif}") String allowedTypesStr,
                       @Value("${media.base.url:http://localhost:8080}") String baseUrl) {
        this.mediaRepository = mediaRepository;
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.allowedTypes = List.of(allowedTypesStr.split(","));
        this.baseUrl = baseUrl;

        try {
            Files.createDirectories(this.storageLocation);
            log.info("Storage directory created at: {}", this.storageLocation);
            log.info("Allowed file types: {}", this.allowedTypes);
            log.info("Base URL for media: {}", this.baseUrl);
        } catch (IOException e) {
            log.error("Could not create storage directory", e);
            throw new StorageException("Could not create storage directory", e);
        }
    }

    public MediaUploadResponse uploadMedia(MultipartFile file, String productId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Only PNG, JPG, and GIF images are accepted");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.storageLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Media media = new Media(
                null,
                uniqueFilename,
                productId
            );

            Media savedMedia = mediaRepository.save(media);

            log.info("Media uploaded successfully: id={}, filename={}, productId={}",
                savedMedia.getId(), originalFilename, productId);

            String downloadUrl = baseUrl + "/api/media/" + savedMedia.getId();

            return new MediaUploadResponse(
                savedMedia.getId(),
                "File uploaded successfully",
                downloadUrl
            );

        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new StorageException("Failed to upload file", e);
        }
    }

    public List<MediaResponse> getAllMedia() {
        return mediaRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<MediaResponse> getMediaByProductId(String productId) {
        return mediaRepository.findByProductId(productId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public MediaResponse getMediaById(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new MediaNotFoundException(MEDIA_NOT_FOUND_MESSAGE + id));
        return toResponse(media);
    }

    public byte[] getMediaFile(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new MediaNotFoundException(MEDIA_NOT_FOUND_MESSAGE + id));

        try {
            Path filePath = this.storageLocation.resolve(media.getFilePath()).normalize();

            // Path traversal protection: ensure the file is within the storage directory
            if (!filePath.startsWith(this.storageLocation)) {
                log.error("Path traversal attempt detected for media id: {}, filePath: {}", id, media.getFilePath());
                throw new SecurityException("Invalid file path");
            }

            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file for media id: {}", id, e);
            throw new StorageException("Failed to read file", e);
        }
    }

    public void deleteMedia(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new MediaNotFoundException(MEDIA_NOT_FOUND_MESSAGE + id));

        try {
            Path filePath = this.storageLocation.resolve(media.getFilePath()).normalize();

            // Path traversal protection: ensure the file is within the storage directory
            if (!filePath.startsWith(this.storageLocation)) {
                log.error("Path traversal attempt detected for media id: {}, filePath: {}", id, media.getFilePath());
                throw new SecurityException("Invalid file path");
            }

            Files.deleteIfExists(filePath);
            mediaRepository.delete(media);
            log.info("Media deleted successfully: id={}", id);
        } catch (IOException e) {
            log.error("Failed to delete file for media id: {}", id, e);
            throw new StorageException("Failed to delete file", e);
        }
    }

    public void deleteAllMediaByProductId(String productId) {
        List<Media> mediaList = mediaRepository.findByProductId(productId);
        for (Media media : mediaList) {
            try {
                Path filePath = this.storageLocation.resolve(media.getFilePath()).normalize();
                if (filePath.startsWith(this.storageLocation)) {
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                log.error("Failed to delete file for media id: {}", media.getId(), e);
            }
        }
        mediaRepository.deleteAll(mediaList);
        log.info("Deleted {} media files for product: {}", mediaList.size(), productId);
    }

    private MediaResponse toResponse(Media media) {
        String downloadUrl = baseUrl + "/api/media/" + media.getId();
        return new MediaResponse(
            media.getId(),
            media.getProductId(),
            downloadUrl
        );
    }
}
