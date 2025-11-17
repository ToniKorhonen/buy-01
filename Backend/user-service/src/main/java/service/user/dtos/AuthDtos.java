package service.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import service.user.models.Role;

public class AuthDtos {
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
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

