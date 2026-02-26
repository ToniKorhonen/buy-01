package service.order.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import service.order.dtos.OrderDtos;
import service.order.exceptions.InsufficientFundsException;
import service.order.exceptions.InsufficientStockException;
import service.order.exceptions.OrderAccessDeniedException;
import service.order.exceptions.OrderNotFoundException;
import service.order.exceptions.ProductNotFoundException;
import service.order.models.Order;
import service.order.models.Status;
import service.order.mongo_repo.OrderRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import service.order.clients.UserServiceClient;
import service.order.clients.ProductServiceClient;

@Service
public class OrderService {

    private static final String PRODUCT_PRICE_KEY = "price";

    private final OrderRepository repo;
    private final RestTemplate restTemplate;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Autowired
    public OrderService(OrderRepository repo, RestTemplate restTemplate,
                        UserServiceClient userServiceClient,
                        ProductServiceClient productServiceClient) {
        this.repo = repo;
        this.restTemplate = restTemplate;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
    }

    // Add product to cart (ADDED status)
    public OrderDtos.OrderResponse addToCart(String buyerId, OrderDtos.AddToCartRequest req) {
        Map<String, Object> product = fetchProduct(req.productId());

        String productName = (String) product.get("name");
        String sellerId = (String) product.get("userId");
        double price = ((Number) product.get(PRODUCT_PRICE_KEY)).doubleValue();
        int availableStock = ((Number) product.get("quantity")).intValue();

        if (req.quantity() > availableStock) {
            throw new InsufficientStockException(
                String.format("Only %d unit(s) of '%s' are available.", availableStock, productName));
        }

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setProductId(req.productId());
        order.setProductName(productName);
        order.setQuantity(req.quantity());
        order.setTotalPrice(price * req.quantity());
        order.setStatus(Status.ADDED);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        return toDto(repo.save(order));
    }

    // Get buyer's cart (ADDED orders)
    public List<OrderDtos.OrderResponse> getCart(String buyerId) {
        return repo.findByBuyerIdAndStatus(buyerId, Status.ADDED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Get buyer's placed/active/history orders (everything except ADDED)
    public List<OrderDtos.OrderResponse> getMyOrders(String buyerId) {
        return repo.findByBuyerId(buyerId).stream()
                .filter(o -> o.getStatus() != Status.ADDED)
                .map(this::toDto)
                .toList();
    }

    // Get seller's incoming orders
    public List<OrderDtos.OrderResponse> getSellerOrders(String sellerId) {
        return repo.findBySellerId(sellerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Update quantity (ADDED only)
    public OrderDtos.OrderResponse updateQuantity(String orderId, String buyerId, int quantity) {
        Order order = findAndCheckBuyer(orderId, buyerId);
        if (order.getStatus() != Status.ADDED) {
            throw new IllegalStateException("Can only update quantity of cart items");
        }
        Map<String, Object> product = fetchProduct(order.getProductId());
        double price = ((Number) product.get(PRODUCT_PRICE_KEY)).doubleValue();
        int availableStock = ((Number) product.get("quantity")).intValue();

        if (quantity > availableStock) {
            throw new InsufficientStockException(
                String.format("Only %d unit(s) of '%s' are available.", availableStock, product.get("name")));
        }

        order.setQuantity(quantity);
        order.setTotalPrice(price * quantity);
        order.setUpdatedAt(Instant.now());
        return toDto(repo.save(order));
    }

    // Buyer pays — ADDED -> STARTED
    public OrderDtos.OrderResponse placeOrder(String orderId, String buyerId) {
        Order order = findAndCheckBuyer(orderId, buyerId);
        if (order.getStatus() != Status.ADDED) {
            throw new IllegalStateException("Only cart items can be placed");
        }

        double balance = userServiceClient.getBalance(buyerId);
        if (balance < order.getTotalPrice()) {
            throw new InsufficientFundsException(
                String.format("Insufficient balance. Required: $%.2f, Available: $%.2f",
                    order.getTotalPrice(), balance));
        }

        int availableStock = productServiceClient.getStock(order.getProductId());
        if (order.getQuantity() > availableStock) {
            throw new InsufficientStockException(
                String.format("Not enough stock for '%s'. Requested: %d, Available: %d",
                    order.getProductName(), order.getQuantity(), availableStock));
        }

        order.setStatus(Status.STARTED);
        order.setUpdatedAt(Instant.now());
        Order saved = repo.save(order);
        userServiceClient.deductFromBuyer(buyerId, order.getTotalPrice());
        productServiceClient.adjustStock(order.getProductId(), -order.getQuantity());
        return toDto(saved);
    }

    // Seller confirms — STARTED -> ONGOING
    public OrderDtos.OrderResponse markOngoing(String orderId, String sellerId) {
        Order order = findAndCheckSeller(orderId, sellerId);
        if (order.getStatus() != Status.STARTED) {
            throw new IllegalStateException("Order must be in STARTED status");
        }
        order.setStatus(Status.ONGOING);
        order.setUpdatedAt(Instant.now());
        return toDto(repo.save(order));
    }

    // Buyer confirms delivery — ONGOING -> DELIVERED
    public OrderDtos.OrderResponse markDelivered(String orderId, String buyerId) {
        Order order = findAndCheckBuyer(orderId, buyerId);
        if (order.getStatus() != Status.ONGOING) {
            throw new IllegalStateException("Order must be in ONGOING status");
        }
        order.setStatus(Status.DELIVERED);
        order.setUpdatedAt(Instant.now());
        Order saved = repo.save(order);
        userServiceClient.creditSeller(order.getSellerId(), order.getTotalPrice());
        return toDto(saved);
    }

    // Cancel order — buyer or seller, refunds money if already paid
    public OrderDtos.OrderResponse cancelOrder(String orderId, String userId) {
        Order order = findById(orderId);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new OrderAccessDeniedException("Not authorized to cancel this order");
        }
        if (order.getStatus() == Status.DELIVERED || order.getStatus() == Status.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a delivered or already cancelled order");
        }
        boolean alreadyPaid = order.getStatus() == Status.STARTED || order.getStatus() == Status.ONGOING;
        order.setStatus(Status.CANCELLED);
        order.setUpdatedAt(Instant.now());
        Order saved = repo.save(order);
        if (alreadyPaid) {
            userServiceClient.refundBuyer(order.getBuyerId(), order.getTotalPrice());
            productServiceClient.adjustStock(saved.getProductId(), saved.getQuantity());
        }
        return toDto(saved);
    }

    // Delete from cart (ADDED only)
    public void deleteOrder(String orderId, String buyerId) {
        Order order = findAndCheckBuyer(orderId, buyerId);
        if (order.getStatus() != Status.ADDED) {
            throw new IllegalStateException("Can only delete cart items");
        }
        repo.delete(order);
    }

    // Reorder — restore a DELIVERED/CANCELLED order to ADDED with new quantity
    public OrderDtos.OrderResponse reorder(String orderId, String buyerId, int quantity) {
        Order order = findAndCheckBuyer(orderId, buyerId);
        if (order.getStatus() != Status.DELIVERED && order.getStatus() != Status.CANCELLED) {
            throw new IllegalStateException("Can only reorder delivered or cancelled orders");
        }
        Map<String, Object> product = fetchProduct(order.getProductId());
        double price = ((Number) product.get(PRODUCT_PRICE_KEY)).doubleValue();
        order.setQuantity(quantity);
        order.setTotalPrice(price * quantity);
        order.setStatus(Status.ADDED);
        order.setUpdatedAt(Instant.now());
        return toDto(repo.save(order));
    }

    // --- Helpers ---

    private Map<String, Object> fetchProduct(String productId) {
        String url = productServiceUrl + "/" + productId;
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        Map<String, Object> product = response.getBody();
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        return product;
    }

    private Order findById(String orderId) {
        return repo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Order findAndCheckBuyer(String orderId, String buyerId) {
        Order order = findById(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderAccessDeniedException("Not authorized to access this order");
        }
        return order;
    }

    private Order findAndCheckSeller(String orderId, String sellerId) {
        Order order = findById(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new OrderAccessDeniedException("Not authorized to access this order");
        }
        return order;
    }

    public OrderDtos.OrderResponse toDto(Order o) {
        return new OrderDtos.OrderResponse(
            o.getOrderId(),
            o.getBuyerId(),
            o.getSellerId(),
            o.getProductId(),
            o.getProductName(),
            o.getQuantity(),
            o.getTotalPrice(),
            o.getStatus().name(),
            o.getCreatedAt() != null ? o.getCreatedAt().toString() : null,
            o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null
        );
    }
}
