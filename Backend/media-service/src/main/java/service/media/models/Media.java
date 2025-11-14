package service.media.models;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "media")
public class Media {
    @Id
    private String id;

    @NotBlank(message = "File path is required")
    private String filePath;

    private String uploaderId;

    // Constructor for creating new Media (used by MediaService)
    public Media(String id, String filePath, String uploaderId) {
        this.id = id;
        this.filePath = filePath;
        this.uploaderId = uploaderId;
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

}

