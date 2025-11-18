package service.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.user.dtos.UserDtos.*;
import service.user.exception.DuplicateResourceException;
import service.user.exception.UserNotFoundException;
import service.user.models.Role;
import service.user.models.User;
import service.user.mongo_repo.UserRepository;

import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.repo = userRepository;
        this.encoder = encoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().toLowerCase();
        if (repo.findByEmail(email) != null) {
            throw new DuplicateResourceException("Email already in use");
        }
        User u = new User();
        u.setName(req.name());
        u.setEmail(email);
        u.setPassword(encoder.encode((req.password())));
        Role role = req.role() == null ? Role.CLIENT : req.role();
        u.setRole(role);
        u.setAvatarId(req.avatarId());
        repo.save(u);
        return toResponse(u);
    }
    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.getAvatarId()
        );
    }


    public UserResponse get(String id) {
        return toResponse(find(id));
    }
    private User find(String id) {
        return repo.findById(id).orElseThrow(() -> new UserNotFoundException(id, "retrieval"));
    }
    public List<UserResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse update(String id, UpdateProfileRequest req) {
        User u = find(id);
        if (req.name() != null && !req.name().isBlank()) {
            u.setName(req.name());
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(encoder.encode(req.password()));
        }
        repo.save(u);
        return toResponse(u);
    }

    @Transactional
    public void delete(String id) {
        User u = find(id);
        repo.delete(u);
    }

    public boolean isCurrentUser(String userId, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User user = find(userId);
        String currentUserEmail = authentication.getName();
        return user.getEmail().equals(currentUserEmail);
    }
}
