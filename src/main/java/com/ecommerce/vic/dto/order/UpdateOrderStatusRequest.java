package com.ecommerce.vic.dto.order;

import com.ecommerce.vic.constants.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status cannot be null")
        OrderStatus status
) {}
