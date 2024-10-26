package com.ecommerce.vic.dto.user;

import com.ecommerce.vic.constants.UserRole;

import java.time.LocalDateTime;


public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String country,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
