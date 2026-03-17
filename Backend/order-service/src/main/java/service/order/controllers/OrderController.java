package service.order.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.order.dtos.OrderDtos;
import service.order.services.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // POST /api/orders — add to cart
    @PostMapping
    public ResponseEntity<OrderDtos.OrderResponse> addToCart(
            @Valid @RequestBody OrderDtos.AddToCartRequest req,
            Authentication auth) {
        return ResponseEntity.status(201).body(orderService.addToCart(auth.getName(), req));
    }

    // GET /api/orders/cart — buyer's cart (ADDED)
    @GetMapping("/cart")
    public List<OrderDtos.OrderResponse> getCart(Authentication auth) {
        return orderService.getCart(auth.getName());
    }

    // GET /api/orders/my-orders — buyer's order history (non-ADDED)
    @GetMapping("/my-orders")
    public List<OrderDtos.OrderResponse> getMyOrders(Authentication auth) {
        return orderService.getMyOrders(auth.getName());
    }

    // GET /api/orders/seller-orders — seller's incoming orders
    @GetMapping("/seller-orders")
    public List<OrderDtos.OrderResponse> getSellerOrders(Authentication auth) {
        return orderService.getSellerOrders(auth.getName());
    }

    // PATCH /api/orders/{id}/quantity — update cart item quantity
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<OrderDtos.OrderResponse> updateQuantity(
            @PathVariable String id,
            @Valid @RequestBody OrderDtos.UpdateQuantityRequest req,
            Authentication auth) {
        return ResponseEntity.ok(orderService.updateQuantity(id, auth.getName(), req.quantity()));
    }

    // PATCH /api/orders/{id}/place — buyer pays (ADDED -> STARTED)
    @PatchMapping("/{id}/place")
    public ResponseEntity<OrderDtos.OrderResponse> placeOrder(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.placeOrder(id, auth.getName()));
    }

    // PATCH /api/orders/{id}/ongoing — seller confirms (STARTED -> ONGOING)
    @PatchMapping("/{id}/ongoing")
    public ResponseEntity<OrderDtos.OrderResponse> markOngoing(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.markOngoing(id, auth.getName()));
    }

    // PATCH /api/orders/{id}/delivered — buyer confirms delivery (ONGOING -> DELIVERED)
    @PatchMapping("/{id}/delivered")
    public ResponseEntity<OrderDtos.OrderResponse> markDelivered(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.markDelivered(id, auth.getName()));
    }

    // PATCH /api/orders/{id}/cancel — cancel by buyer or seller
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderDtos.OrderResponse> cancelOrder(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(id, auth.getName()));
    }

    // DELETE /api/orders/{id} — remove from cart (ADDED only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String id,
            Authentication auth) {
        orderService.deleteOrder(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/orders/{id}/reorder — reorder with new quantity
    @PatchMapping("/{id}/reorder")
    public ResponseEntity<OrderDtos.OrderResponse> reorder(
            @PathVariable String id,
            @Valid @RequestBody OrderDtos.UpdateQuantityRequest req,
            Authentication auth) {
        return ResponseEntity.ok(orderService.reorder(id, auth.getName(), req.quantity()));
    }
}

