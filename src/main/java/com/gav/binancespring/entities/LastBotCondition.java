package com.gav.binancespring.entities;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LastBotCondition {
    private final BigDecimal lastMaxPrice;
    private final BigDecimal lastMinPrice;
    private final BigDecimal upMoveDiffLevel;
    private final BigDecimal downMoveDiffLevel;
    private final Long transactionCount;
}
