package com.gav.binancespring.entities;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private Long id;
    private LocalDateTime time;
    private String type;
    private BigDecimal price;
    private BigDecimal volume;
    private BigDecimal commission;
    private BigDecimal btc;
    private BigDecimal usdt;
    private BigDecimal bnb;
}
