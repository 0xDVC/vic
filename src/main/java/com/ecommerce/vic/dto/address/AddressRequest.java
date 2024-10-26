package com.ecommerce.vic.dto.address;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank(message = "Street address is required")
        String streetAddress,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        String country,

        boolean isDefault
) {}
