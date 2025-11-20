package service.user.dtos;

import jakarta.validation.constraints.*;
import service.user.models.Role;

public class UserDtos {
    public record CreateUserRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name can only contain letters, spaces, hyphens and apostrophes")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 255, message = "Email is too long")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
            @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
                    message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
            String password,

            @NotNull(message = "Role is required")
            Role role,

            @Size(max = 255, message = "Avatar ID is too long")
            String avatarId
    ) {}

    // New DTO to update the user's own profile
    public record UpdateProfileRequest(
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Name can only contain letters, spaces, hyphens and apostrophes")
            String name,

            @Email(message = "Email must be valid")
            @Size(max = 255, message = "Email is too long")
            String email,

            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
            @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
                    message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
            String password,

            @Size(max = 255, message = "Avatar ID is too long")
            String avatarId
    ) {}

    public record UserResponse(
            String id,
            String name,
            String email,
            Role role,
            String avatarId
    ) {}
}