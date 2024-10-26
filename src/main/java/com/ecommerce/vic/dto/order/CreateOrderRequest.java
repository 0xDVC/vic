package com.ecommerce.vic.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Order items cannot be empty")
        List<@Valid OrderItemRequest> items,

        @NotNull(message = "Shipping address is required")
        String shippingAddress
) {}
