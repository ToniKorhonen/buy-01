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

    private String productId; // Optional field to link media to a product

    // Default constructor for MongoDB deserialization
    public Media() {
    }

    // Constructor with all fields
    public Media(String id, String filePath, String productId) {
        this.id = id;
        this.filePath = filePath;
        this.productId = productId;
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


    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}

