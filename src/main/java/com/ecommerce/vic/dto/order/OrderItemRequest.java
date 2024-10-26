package com.ecommerce.vic.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity must be specified")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
