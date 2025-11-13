package service.media.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import service.media.dtos.MediaDtos.*;
import service.media.models.Media;
import service.media.mongo_repo.MediaRepository;

import java.io.File;
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

    private final MediaRepository mediaRepository;
    private final Path storageLocation;

    @Autowired
    public MediaService(MediaRepository mediaRepository,
                       @Value("${media.storage.path:./uploads}") String storagePath) {
        this.mediaRepository = mediaRepository;
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageLocation);
            log.info("Storage directory created at: {}", this.storageLocation);
        } catch (IOException e) {
            log.error("Could not create storage directory", e);
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public MediaUploadResponse uploadMedia(MultipartFile file, String uploaderId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Save file to disk
            Path targetLocation = this.storageLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata to database
            Media media = new Media(
                null,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                uniqueFilename,
                uploaderId
            );

            Media savedMedia = mediaRepository.save(media);
            log.info("Media uploaded successfully: id={}, filename={}", savedMedia.getId(), originalFilename);

            String downloadUrl = "/api/media/" + savedMedia.getId();

            return new MediaUploadResponse(
                savedMedia.getId(),
                "File uploaded successfully",
                downloadUrl
            );

        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public List<MediaResponse> getAllMedia() {
        return mediaRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<MediaResponse> getMediaByUploaderId(String uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public MediaResponse getMediaById(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));
        return toResponse(media);
    }

    public byte[] getMediaFile(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        try {
            Path filePath = this.storageLocation.resolve(media.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file for media id: {}", id, e);
            throw new RuntimeException("Failed to read file", e);
        }
    }

    public void deleteMedia(String id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        try {
            Path filePath = this.storageLocation.resolve(media.getFilePath());
            Files.deleteIfExists(filePath);
            mediaRepository.delete(media);
            log.info("Media deleted successfully: id={}", id);
        } catch (IOException e) {
            log.error("Failed to delete file for media id: {}", id, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private MediaResponse toResponse(Media media) {
        String downloadUrl = "/api/media/" + media.getId();
        return new MediaResponse(
            media.getId(),
            media.getFilename(),
            media.getContentType(),
            media.getFileSize(),
            media.getUploaderId(),
            media.getUploadDate(),
            downloadUrl
        );
    }
}
