package service.order.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Autowired
    public ProductServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Decrease stock when an order is placed (delta is negative).
     * Restore stock when an order is cancelled before delivery (delta is positive).
     */
    public void adjustStock(String productId, int delta) {
        String url = productServiceUrl + "/internal/stock/" + productId;
        try {
            restTemplate.patchForObject(url, Map.of("delta", delta), Void.class);
            logger.info("Adjusted stock for product {}: delta={}", productId, delta);
        } catch (RestClientException e) {
            // Log only — stock adjustment failure should not roll back an order transition
            logger.error("Failed to adjust stock for product {}: {}", productId, e.getMessage());
        }
    }
}
