package com.gav.binancespring.services.handlers;

import com.gav.binancespring.config.BinanceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class BookTickerWSHandler implements WebSocketHandler {

    private final BookTickerMessageHandler bookTickerMessageHandler;
    private final Runnable onClosed;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong lastMessageAt = new AtomicLong();
    private final BinanceProperties properties;
    private ScheduledFuture<?> watchdog;

    public BookTickerWSHandler(BookTickerMessageHandler bookTickerMessageHandler, Runnable onClosed, ScheduledExecutorService scheduler, BinanceProperties properties) {
        this.bookTickerMessageHandler = bookTickerMessageHandler;
        this.onClosed = onClosed;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        lastMessageAt.set(System.nanoTime());
        watchdog = scheduler.scheduleWithFixedDelay(
                () -> checkIdle(session),
                10,
                10,
                TimeUnit.SECONDS);
        log.info("afterConnectionEstablished bookTicker");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        lastMessageAt.set(System.nanoTime());
        bookTickerMessageHandler.handleMessage(message.getPayload().toString());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("handleTransportError" + "\t" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        if (watchdog != null) {
            watchdog.cancel(false);
        }
        log.warn("afterConnectionClosed" + "\t" + closeStatus);
        onClosed.run();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void checkIdle(WebSocketSession session) {
        long currentDelay = System.nanoTime() - lastMessageAt.get();
        if (currentDelay <= TimeUnit.SECONDS.toNanos(properties.retryMaxDelay()) || !session.isOpen()) {
            return;
        } else {
            log.warn("Watchdog: Нет данных, закрываем сессию");
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
