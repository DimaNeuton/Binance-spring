package com.gav.binancespring.dto;

import java.math.BigDecimal;

public record OrderResult(
        String id,
        int status,
        BigDecimal price,
        BigDecimal executedQty,
        long transactTime,
        String errorMessage
) {
}
