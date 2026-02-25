package service.user.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import service.user.clients.ProductServiceClient;
import service.user.clients.MediaServiceClient;
import service.user.dtos.UserDtos.UserResponse;
import service.user.dtos.UserDtos.UpdateProfileRequest;
import service.user.models.User;
import service.user.mongo_repo.UserRepository;
import service.user.security.JwtService;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ProductServiceClient productServiceClient;
    private final MediaServiceClient mediaServiceClient;

    @Autowired
    public ProfileController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder, ProductServiceClient productServiceClient, MediaServiceClient mediaServiceClient) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.productServiceClient = productServiceClient;
        this.mediaServiceClient = mediaServiceClient;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
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
            user.getAvatarId(),
            user.getBalance(),
            user.getMoneySpent(),
            user.getMoneyReceived()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
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

        // Update name if provided
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }

        // Update email if provided and different
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(user.getEmail())) {
            // Check if new email already exists
            User existingUser = userRepository.findByEmail(request.email());
            if (existingUser != null) {
                return ResponseEntity.status(409).build(); // Conflict
            }
            user.setEmail(request.email());
        }

        // Update password if provided
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        // Update avatarId if provided
        if (request.avatarId() != null) {
            user.setAvatarId(request.avatarId());
        }

        userRepository.save(user);

        UserResponse response = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatarId(),
            user.getBalance(),
            user.getMoneySpent(),
            user.getMoneyReceived()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/topup")
    public ResponseEntity<UserResponse> topUp(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TopUpRequest request) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.status(401).build();
        }
        if (request.amount() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        if (email == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(email);
        if (user == null) return ResponseEntity.status(404).build();

        user.setBalance(user.getBalance() + request.amount());
        userRepository.save(user);

        return ResponseEntity.ok(new UserResponse(
            user.getId(), user.getName(), user.getEmail(), user.getRole(),
            user.getAvatarId(), user.getBalance(), user.getMoneySpent(), user.getMoneyReceived()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@RequestHeader("Authorization") String authHeader) {        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
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

        // Delete user avatar if it exists
        if (user.getAvatarId() != null && !user.getAvatarId().isBlank()) {
            mediaServiceClient.deleteMedia(user.getAvatarId());
        }

        // Delete all products created by this user (which will cascade delete product images)
        productServiceClient.deleteAllProductsByUserId(user.getId());

        // Delete the user account
        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    // Internal endpoint called by order-service to update wallet fields
    // Not exposed through API gateway
    @PatchMapping("/internal/wallet/{userId}")
    public ResponseEntity<Void> updateWallet(
            @PathVariable String userId,
            @RequestBody WalletUpdateRequest request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (request.balanceDelta() != 0) {
            user.setBalance(user.getBalance() + request.balanceDelta());
        }
        if (request.spentDelta() != 0) {
            user.setMoneySpent(user.getMoneySpent() + request.spentDelta());
        }
        if (request.receivedDelta() != 0) {
            user.setMoneyReceived(user.getMoneyReceived() + request.receivedDelta());
        }
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // Internal endpoint called by order-service to check buyer balance before checkout
    // Not exposed through API gateway
    @GetMapping("/internal/balance/{userId}")
    public ResponseEntity<Double> getBalance(@PathVariable String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user.getBalance());
    }

    public record WalletUpdateRequest(double balanceDelta, double spentDelta, double receivedDelta) {}

    public record TopUpRequest(double amount) {}
}
