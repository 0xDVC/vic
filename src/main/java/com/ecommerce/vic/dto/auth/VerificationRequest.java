package com.ecommerce.vic.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
    @NotBlank(message = "Verification code is required")
    String code,
    
    @NotBlank(message = "Email is required")
    String email
) {}