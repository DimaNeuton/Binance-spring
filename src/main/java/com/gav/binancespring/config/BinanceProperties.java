package com.gav.binancespring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "binance")
public record BinanceProperties(
        String bookTickerUrl,
        String orderUrl,
        String restUrl,
        long reconnectDelay,
        int retryMaxAttempts,
        Duration retryInitialDelay,
        long retryMaxDelay,
        BigDecimal commissionRatio,
        String apiKey,
        String privateKeyPath) {
}
