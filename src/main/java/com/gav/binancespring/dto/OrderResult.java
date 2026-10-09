package com.gav.binancespring.dto;

import java.math.BigDecimal;

public record OrderResult(
        String id,
        int status,
        BigDecimal price,
        BigDecimal executedQty,
        BigDecimal commission,
        long transactTime,
        String errorMessage
) {}
