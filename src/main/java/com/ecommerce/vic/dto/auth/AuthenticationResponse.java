package com.ecommerce.vic.dto.auth;

import lombok.Builder;

@Builder
public class AuthenticationResponse{
    String token;

    @Builder.Default
    boolean requiresVerification = false;

    @Builder.Default
    boolean isVerified = false;

    @Builder.Default
    String message = "Authentication successful";
}
