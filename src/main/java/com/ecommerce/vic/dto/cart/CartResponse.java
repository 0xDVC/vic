package com.ecommerce.vic.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        Integer totalItems
) {}

