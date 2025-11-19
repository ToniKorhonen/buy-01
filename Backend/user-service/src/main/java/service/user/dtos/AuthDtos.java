package service.user.dtos;

import jakarta.validation.constraints.*;
import service.user.models.Role;

public class AuthDtos {
    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 255, message = "Email is too long")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 1, max = 128, message = "Password is too long")
            String password
    ) {}

    public record LoginResponse(
            String token,
            UserInfo user
    ) {}

    public record UserInfo(
            String id,
            String name,
            String email,
            Role role
    ) {}
}

