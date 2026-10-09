package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceProperties;
import com.gav.binancespring.services.handlers.BookTickerMessageHandler;
import com.gav.binancespring.services.handlers.BookTickerWSHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Slf4j
public class BookTickerStreamService implements ApplicationRunner {

    private final BookTickerMessageHandler bookTickerMessageHandler;
    private final WebSocketClient bookTickerWebSocketClient;
    private final ScheduledExecutorService bookTickerWebsocketScheduler;
    private final BinanceProperties properties;
    private volatile boolean running = true;
    private volatile boolean connecting;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        connect();
    }

    private void connect() {

        if (!running || connecting) {
            return;
        }
        connecting = true;

        var session = bookTickerWebSocketClient
                .execute(new BookTickerWSHandler(
                        bookTickerMessageHandler,
                        this::onClosed,
                        bookTickerWebsocketScheduler,
                        properties), properties.bookTickerUrl())
                .whenComplete((sess, error) -> {
                    connecting = false;
                    if (error != null && running) {
                        log.warn("Сокет не открылся: " + error.getMessage());
                        log.info("Переподключаемся");
                        reconnect();
                        error.getCause();
                    } else {
                        log.info("Сессия bookTicker открыта");
                    }
                });
    }

    void onClosed() {
        if (running) {
            reconnect();
        }
    }

    private void reconnect() {
        long delay = properties.reconnectDelay();
        bookTickerWebsocketScheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        bookTickerWebsocketScheduler.shutdown();
    }
}
