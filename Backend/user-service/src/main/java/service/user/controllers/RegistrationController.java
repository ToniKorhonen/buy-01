package service.user.controllers;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.user.dtos.UserDtos.CreateUserRequest;
import service.user.dtos.UserDtos.UserResponse;
import service.user.exception.DuplicateResourceException;
import service.user.services.UserService;

@RestController
@RequestMapping("/auth")
public class RegistrationController {
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    private final UserService userService;

    @Autowired
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserRequest req) {
        try {
            UserResponse created = userService.create(req);
            log.info("[REGISTER] User created: id={}, email={}", created.id(), created.email());
            return ResponseEntity.ok(created);
        } catch (DuplicateResourceException | DuplicateKeyException e) {
            log.warn("[REGISTER] Duplicate email: {}", req.email());
            return ResponseEntity.status(409).body("Email already in use");
        } catch (Exception e) {
            log.error("[REGISTER] Internal error for email={}", req.email(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}