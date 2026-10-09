package com.gav.binancespring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class WebsocketClientConfig {

    @Bean
    public StandardWebSocketClient bookTickerWebSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService bookTickerWebsocketScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    public StandardWebSocketClient orderWebSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService orderWebsocketScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
