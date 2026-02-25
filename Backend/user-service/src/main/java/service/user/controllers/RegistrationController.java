package service.user.controllers;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.user.dtos.UserDtos.CreateUserRequest;
import service.user.dtos.UserDtos.UserResponse;
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
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest req) {
        UserResponse created = userService.create(req);
        log.info("[REGISTER] User created: id={}, email={}", created.id(), created.email());
        return ResponseEntity.ok(created);
    }
}