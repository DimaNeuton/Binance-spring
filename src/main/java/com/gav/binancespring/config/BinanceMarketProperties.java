package com.gav.binancespring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "binance.market")
public record BinanceMarketProperties(
        String baseUrl,
        String symbol,
        String stream,
        Duration reconnectDelay,
        int retryMaxAttempts,
        Duration retryInitialDelay,
        Duration retryMaxDelay,
        BigDecimal commissionRatio) {


    public String streamUrl() {
        return baseUrl + "/" + symbol + "@" + stream;
    }


}
