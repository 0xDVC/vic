package com.ecommerce.vic.dto.order;

import java.math.BigDecimal;

public record OrderStats(
        long orderCount,
        BigDecimal totalRevenue,
        Double averageOrderValue
) {}
