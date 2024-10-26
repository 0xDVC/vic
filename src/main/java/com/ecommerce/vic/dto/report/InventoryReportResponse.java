package com.ecommerce.vic.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryReportResponse(
        LocalDate generatedDate,
        List<ProductInventoryInfo> products,
        BigDecimal totalInventoryValue,
        long lowStockItemsCount
) {}
