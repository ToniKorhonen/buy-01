package service.user.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.user.dtos.UserDtos.UserResponse;
import service.user.models.User;
import service.user.mongo_repo.UserRepository;
import service.user.security.JwtService;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Autowired
    public ProfileController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        UserResponse response = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatarId()
        );

        return ResponseEntity.ok(response);
    }
}

