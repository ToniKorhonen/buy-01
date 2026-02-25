package service.order.mongo_repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.order.models.Order;
import service.order.models.Status;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    // Buyer: get all orders with a specific status (e.g. cart = ADDED)
    List<Order> findByBuyerIdAndStatus(String buyerId, Status status);

    // Buyer: get all orders regardless of status
    List<Order> findByBuyerId(String buyerId);

    // Seller: get all orders for their products
    List<Order> findBySellerId(String sellerId);

    // Seller: get orders by status
    List<Order> findBySellerIdAndStatus(String sellerId, Status status);
}
