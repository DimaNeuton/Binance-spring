package com.gav.binancespring.services.handlers;

import com.gav.binancespring.config.BinanceProperties;
import com.gav.binancespring.services.BinanceSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class OrderWSHandler implements WebSocketHandler {

    private final BinanceSignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final OrderMessageHandler orderMessageHandler;
    private final Runnable onClosed;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong lastMessageAt = new AtomicLong();
    private final BinanceProperties properties;
    private ScheduledFuture<?> watchdog;

    public OrderWSHandler(BinanceSignatureService signatureService, ObjectMapper objectMapper,
                          OrderMessageHandler orderMessageHandler, Runnable onClosed,
                          ScheduledExecutorService scheduler, BinanceProperties properties) {
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
        this.orderMessageHandler = orderMessageHandler;
        this.onClosed = onClosed;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        lastMessageAt.set(System.nanoTime());
//        watchdog = scheduler.scheduleWithFixedDelay(
//                () -> checkIdle(session),
//                10,
//                10,
//                TimeUnit.SECONDS);
        log.info("afterConnectionEstablished orders");
        logon(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
//        lastMessageAt.set(System.nanoTime());
        orderMessageHandler.handleMessage(message.getPayload().toString());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("handleTransportError" + "\t" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
//        if (watchdog != null) {
//            watchdog.cancel(false);
//        }
        log.warn("afterConnectionClosed" + "\t" + closeStatus);
        onClosed.run();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void checkIdle(WebSocketSession session) {
        long idleNs = System.nanoTime() - lastMessageAt.get();
        if (idleNs <= TimeUnit.SECONDS.toNanos(properties.retryMaxDelay()) || !session.isOpen()) {
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

    private void logon(WebSocketSession session) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("apiKey", properties.apiKey());
        params.put("timestamp", System.currentTimeMillis());
        params.put("signature", signatureService.sign(params));

        Map<String, Object> request = Map.of(
                "id", UUID.randomUUID().toString(),
                "method", "session.logon",
                "params", params);

        String json = objectMapper.writeValueAsString(request);
//        log.info("sendMessage: " + json);
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(WebSocketSession session, Map<String, Object> params, String method, String id) {

        params.put("timestamp", System.currentTimeMillis());

        Map<String, Object> request = Map.of(
                "id", id,
                "method", method,
                "params", params);

        String json = objectMapper.writeValueAsString(request);

        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}