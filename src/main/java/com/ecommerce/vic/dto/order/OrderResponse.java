package com.ecommerce.vic.dto.order;

import com.ecommerce.vic.dto.user.PartialUserResponse;
import com.ecommerce.vic.dto.user.UserResponse;
import com.ecommerce.vic.constants.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,

        String orderNumber,

        PartialUserResponse user,

        List<OrderItemResponse> items,

        BigDecimal totalAmount,

        OrderStatus status,

        String shippingAddress,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    // Example JSON response:
    /*
    {
        "id": 1,
        "orderNumber": "ORD-2024-0001",
        "user": {
            "id": 1,
            "firstName": "John",
            "lastName": "Doe",
            "email": "john@example.com",
            "phoneNumber": "+1234567890",
            "role": "CUSTOMER",
            "createdAt": "2024-03-15 10:30:00",
            "updatedAt": "2024-03-15 10:30:00"
        },
        "items": [...],
        "totalAmount": 299.99,
        "status": "PROCESSING",
        "shippingAddress": "123 Main St, City, Country",
        "createdAt": "2024-03-15 10:30:00",
        "updatedAt": "2024-03-15 10:35:00"
    }
    */
}
