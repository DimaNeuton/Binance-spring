package com.gav.binancespring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "binance.trade")
public record BinanceTradeProperties(
        String url,
        String restUrl,
        String apiKey,
        String privateKeyPath,
        Duration reconnectDelay,
        int retryMaxAttempts,
        Duration retryInitialDelay,
        Duration retryMaxDelay,
        BigDecimal commissionRatio) {

}
