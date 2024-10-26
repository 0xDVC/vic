package com.ecommerce.vic.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        String category,
        String size,
        String adminName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
