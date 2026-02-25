package service.order.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderDtos {

    // Buyer sends this to add a product to cart
    public record AddToCartRequest(
        @NotBlank(message = "Product ID is required")
        String productId,

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
    ) {}

    // Buyer sends this to update quantity in cart
    public record UpdateQuantityRequest(
        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
    ) {}

    // Response sent back to frontend - matches frontend OrderResponse interface
    public record OrderResponse(
        String id,
        String buyerId,
        String sellerId,
        String productId,
        String productName,
        int quantity,
        double totalPrice,
        String status,
        String createdAt,
        String updatedAt
    ) {}
}

