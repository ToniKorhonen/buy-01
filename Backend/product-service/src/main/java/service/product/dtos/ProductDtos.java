package service.product.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ProductDtos {
    public record CreateProductRequest(
            @NotBlank String name,
            String description,
            @Positive double price,
            @Positive int quantity,
            String userId
    ) {}
    public record UpdateProductRequest(
            @NotBlank String name,
            String description,
            @Positive double price,
            @Positive int quantity,
            String userId
    ) {}
    public record ProductResponse(
            String id,
            String name,
            String description,
            double price,
            String userId
    ) {}
}
