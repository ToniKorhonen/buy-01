package service.user.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProductServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8082}")
    private String productServiceUrl;

    @Autowired
    public ProductServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void deleteAllProductsByUserId(String userId) {
        try {
            String url = productServiceUrl + "/user/" + userId;
            restTemplate.delete(url);
            logger.info("Successfully deleted all products for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to delete products for user: {}. Error: {}", userId, e.getMessage());
        }
    }
}

