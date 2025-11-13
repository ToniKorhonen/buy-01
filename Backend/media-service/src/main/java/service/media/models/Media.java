package service.media.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "media")
public class Media {
    @Id
    private String id;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @NotBlank(message = "File path is required")
    private String filePath;

    private String uploaderId;

    private LocalDateTime uploadDate;

    // Constructor for creating new Media (used by MediaService)
    public Media(String id, String filename, String contentType, Long fileSize, String filePath, String uploaderId) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploaderId = uploaderId;
        this.uploadDate = LocalDateTime.now();
    }

    // Default constructor for MongoDB deserialization
    public Media() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}

