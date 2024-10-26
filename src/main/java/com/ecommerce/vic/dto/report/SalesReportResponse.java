package com.ecommerce.vic.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SalesReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal averageOrderValue,
        Map<String, Long> productsSold
) {}
