package com.ecommerce.vic.dto.user;

import com.ecommerce.vic.exception.InvalidPasswordException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String newPassword,

            @NotBlank(message = "Password confirmation is required")
            String confirmPassword
    ) {
    public ChangePasswordRequest {
        if (newPassword != null && !newPassword.equals(confirmPassword)) {
            throw new InvalidPasswordException("New password and confirmation do not match");
        }
    }
}
