package service.order.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import service.order.exceptions.OrderServiceException;

import java.util.Map;

@Service
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Autowired
    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Fetch a user's current balance from user-service
    public double getBalance(String userId) {
        String url = userServiceUrl + "/profile/internal/balance/" + userId;
        try {
            Double balance = restTemplate.getForObject(url, Double.class);
            return balance != null ? balance : 0.0;
        } catch (RestClientException e) {
            throw new OrderServiceException("Could not retrieve balance for user: " + userId, e);
        }
    }

    // Adjust buyer's balance and moneySpent
    public void deductFromBuyer(String buyerId, double amount) {
        updateWallet(buyerId, -amount, amount, 0);
    }

    // Refund buyer (cancel)
    public void refundBuyer(String buyerId, double amount) {
        updateWallet(buyerId, amount, -amount, 0);
    }

    // Credit seller on delivery
    public void creditSeller(String sellerId, double amount) {
        updateWallet(sellerId, amount, 0, amount);
    }

    private void updateWallet(String userId, double balanceDelta, double spentDelta, double receivedDelta) {
        String url = userServiceUrl + "/profile/internal/wallet/" + userId;
        Map<String, Double> body = Map.of(
            "balanceDelta", balanceDelta,
            "spentDelta", spentDelta,
            "receivedDelta", receivedDelta
        );
        try {
            restTemplate.patchForObject(url, body, Void.class);
            logger.info("Updated wallet for user {}: balance={}, spent={}, received={}",
                userId, balanceDelta, spentDelta, receivedDelta);
        } catch (RestClientException e) {
            // Log only — wallet update failure should not roll back a completed order transition
            logger.error("Failed to update wallet for user {}: {}", userId, e.getMessage());
        }
    }
}
