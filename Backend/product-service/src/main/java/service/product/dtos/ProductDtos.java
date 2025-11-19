package service.product.dtos;

import jakarta.validation.constraints.*;

public class ProductDtos {
    public record CreateProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
            @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()&]+$", message = "Product name contains invalid characters")
            String name,

            @Size(max = 2000, message = "Description must not exceed 2000 characters")
            String description,

            @NotNull(message = "Price is required")
            @Positive(message = "Price must be positive")
            @DecimalMax(value = "1000000.00", message = "Price must not exceed 1,000,000")
            @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
            double price,

            @NotNull(message = "Quantity is required")
            @Min(value = 0, message = "Quantity cannot be negative")
            @Max(value = 100000, message = "Quantity must not exceed 100,000")
            int quantity,

            String userId
    ) {}

    public record UpdateProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
            @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()&]+$", message = "Product name contains invalid characters")
            String name,

            @Size(max = 2000, message = "Description must not exceed 2000 characters")
            String description,

            @NotNull(message = "Price is required")
            @Positive(message = "Price must be positive")
            @DecimalMax(value = "1000000.00", message = "Price must not exceed 1,000,000")
            @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
            double price,

            @NotNull(message = "Quantity is required")
            @Min(value = 0, message = "Quantity cannot be negative")
            @Max(value = 100000, message = "Quantity must not exceed 100,000")
            int quantity,

            String userId
    ) {}

    public record ProductResponse(
            String id,
            String name,
            String description,
            double price,
            int quantity,
            String userId
    ) {}
}
