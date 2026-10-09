package com.gav.binancespring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ответ Binance на GET /api/v3/order?symbol=...&origClientOrderId=...
 * (и на GET /api/v3/order?symbol=...&orderId=...).
 *
 * Используется для сверки судьбы ордера, когда WS-ответ потерян.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusResponse(

        String symbol,

        @JsonProperty("orderId")
        Long orderId,

        @JsonProperty("orderListId")
        Long orderListId,

        @JsonProperty("clientOrderId")
        String clientOrderId,

        @JsonProperty("origClientOrderId")
        String origClientOrderId,

        @JsonProperty("transactTime")
        Long transactTime,

        @JsonProperty("price")
        BigDecimal price,

        @JsonProperty("origQty")
        BigDecimal origQty,

        @JsonProperty("executedQty")
        BigDecimal executedQty,

        @JsonProperty("cummulativeQuoteQty")
        BigDecimal cummulativeQuoteQty,

        /** NEW, PARTIALLY_FILLED, FILLED, CANCELED, PENDING_CANCEL, REJECTED, EXPIRED */
        @JsonProperty("status")
        String status,

        /** LIMIT, MARKET, STOP_LOSS, ... */
        @JsonProperty("type")
        String type,

        /** BUY, SELL */
        @JsonProperty("side")
        String side,

        @JsonProperty("timeInForce")
        String timeInForce,

        @JsonProperty("icebergQty")
        BigDecimal icebergQty,

        @JsonProperty("time")
        Long time,

        @JsonProperty("updateTime")
        Long updateTime,

        @JsonProperty("isWorking")
        Boolean isWorking,

        @JsonProperty("workingTime")
        Long workingTime,

        @JsonProperty("selfTradePreventionMode")
        String selfTradePreventionMode,

        /**
         * Поле присутствует только при newOrderRespType=FULL.
         * В ответе на GET /api/v3/order Binance его обычно НЕ возвращает.
         * Оставлено для унификации с OrderResult.
         */
        @JsonProperty("fills")
        List<Fill> fills
) {

    /**
     * Одна сделка (fill) в составе ответа.
     * commission берётся в том активе, который указан в commissionAsset.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fill(
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("qty") BigDecimal qty,
            @JsonProperty("commission") BigDecimal commission,
            @JsonProperty("commissionAsset") String commissionAsset,
            @JsonProperty("tradeId") Long tradeId
    ) {}

    // ---------- Удобные методы ----------

    /** Исполнено ли полностью. */
    public boolean isFilled() {
        return "FILLED".equals(status);
    }

    /** Ордер существует и ещё активен (не исполнен и не закрыт). */
    public boolean isOpen() {
        return "NEW".equals(status) || "PARTIALLY_FILLED".equals(status);
    }

    /** Ордер отменён, истёк или отклонён — исполнения не будет. */
    public boolean isClosed() {
        return "CANCELED".equals(status)
                || "EXPIRED".equals(status)
                || "REJECTED".equals(status);
    }

    /**
     * Средняя цена исполнения.
     * null, если ордер ещё не исполнен или cummulativeQuoteQty == 0.
     */
    public BigDecimal averagePrice() {
        if (executedQty == null || cummulativeQuoteQty == null) {
            return null;
        }
        if (executedQty.signum() == 0) {
            return null;
        }
        return cummulativeQuoteQty.divide(executedQty, 8, java.math.RoundingMode.HALF_DOWN);
    }

    /**
     * Суммарная комиссия по всем fills.
     * null, если fills отсутствуют (обычный ответ GET /api/v3/order).
     */
    public BigDecimal totalCommission() {
        if (fills == null || fills.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Fill f : fills) {
            if (f.commission() != null) {
                total = total.add(f.commission());
            }
        }
        return total;
    }
}