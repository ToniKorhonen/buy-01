package service.user.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MediaServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(MediaServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${media.service.url:http://localhost:8083}")
    private String mediaServiceUrl;

    @Autowired
    public MediaServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void deleteMedia(String mediaId) {
        try {
            String url = mediaServiceUrl + "/api/media/" + mediaId;
            restTemplate.delete(url);
            logger.info("Successfully deleted media: {}", mediaId);
        } catch (Exception e) {
            logger.error("Failed to delete media: {}. Error: {}", mediaId, e.getMessage());
        }
    }
}

