package com.gav.binancespring.entities;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
public class Balance {
    private final BigDecimal btc;
    private final BigDecimal usdt;
    private final BigDecimal bnb;
}
