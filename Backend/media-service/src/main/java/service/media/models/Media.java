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
    private final String id;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @NotBlank(message = "File path is required")
    private String filePath;

    private final String uploaderId;

    private final LocalDateTime uploadDate;

    public Media(String id, String filename, String contentType, Long fileSize, String filePath, String uploaderId) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploaderId = uploaderId;
        this.uploadDate = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
}

