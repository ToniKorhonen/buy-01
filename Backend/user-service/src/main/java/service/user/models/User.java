package service.user.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
            message = "Password must contain at least one number and one special character"
    )
    private String password;
    private Role role;

    private String avatar; //optional field for user avatar URL

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }

    public void setPassword(@NotBlank @Size(min = 8) String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
