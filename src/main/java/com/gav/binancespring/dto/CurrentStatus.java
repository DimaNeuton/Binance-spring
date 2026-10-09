package com.gav.binancespring.dto;

import com.gav.binancespring.entities.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;

public record CurrentStatus(
        BigDecimal currentProfit,
        Long closedTransactions,
        Long totalTransactions,
        BigDecimal tradingVolume,
        BigDecimal balanceBtcReal,
        BigDecimal balanceUsdtReal,
        BigDecimal balanceBnbReal,
        BigDecimal balanceBtcDb,
        BigDecimal balanceUsdtDb,
        BigDecimal balanceBnbDb,
        Instant uptime,
        Transaction transaction1,
        Transaction transaction2,
        Transaction transaction3,
        TreeMap<Long, BigDecimal> profits) {}
