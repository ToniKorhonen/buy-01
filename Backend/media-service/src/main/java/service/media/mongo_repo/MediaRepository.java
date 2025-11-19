package service.media.mongo_repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.media.models.Media;

import java.util.List;

public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductId(String productId);
}