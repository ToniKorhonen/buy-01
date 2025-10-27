package service.product.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class ProductDtos {
    public record CreateProductRequest(
            @NotBlank String name,
            String description,
            @Positive double price,
            @Positive int quantity,
            List<String> images
    ) {}
    public record UpdateProductRequest(
            @NotBlank String name,
            String description,
            @Positive double price,
            @Positive int quantity,
            List<String> images
    ) {}
    public record ProductResponse(
            String id,
            String name,
            String description,
            double price,
            int quantity,
            String userId,
            String ownerName,
            List<String> images
    ) {}
}
