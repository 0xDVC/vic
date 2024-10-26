package com.ecommerce.vic.dto.report;

import java.math.BigDecimal;

public record ProductInventoryInfo(
        Long id,
        String name,
        int currentStock,
        BigDecimal price,
        boolean reorderNeeded
) {}
