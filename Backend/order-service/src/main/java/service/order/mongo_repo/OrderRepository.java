package service.order.mongo_repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.order.models.Order;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByOrderId(String orderId);
}
