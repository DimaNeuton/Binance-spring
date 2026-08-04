package com.gav.binancespring.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TradeHistoryRow (
        Long expectedId,
        LocalDateTime expectedTime,
        String expectedType,
        BigDecimal expectedPrice,
        BigDecimal expectedVolume,
        BigDecimal expectedCommission,
        BigDecimal expectedBtc,
        BigDecimal expectedUsdt,
        BigDecimal expectedBnb,
        BigDecimal expectedProfit,
        Long id,
        LocalDateTime time,
        String type,
        BigDecimal price,
        BigDecimal volume,
        BigDecimal commission,
        BigDecimal btc,
        BigDecimal usdt,
        BigDecimal bnb,
        BigDecimal timeSlippage,
        BigDecimal priceSlippage,
        BigDecimal profit) {}


