package com.playvora.playvora_api.user.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @Schema(description = "User email", example = "john.doe@example.com")
    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User password", example = "password123")
    @NotEmpty(message = "Password is required")
    private String password;

    @Schema(description = "User nickname", example = "john_doe")
    private String nickname;

    @Schema(description = "User first name", example = "John")
    @NotEmpty(message = "First name is required")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    @NotEmpty(message = "Last name is required")
    private String lastName;

}
