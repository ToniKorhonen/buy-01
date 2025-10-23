package service.user.mongo_repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import service.user.models.User;

public interface UserRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
}