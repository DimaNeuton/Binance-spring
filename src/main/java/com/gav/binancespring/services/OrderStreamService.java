package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceProperties;
import com.gav.binancespring.dto.OrderResult;
import com.gav.binancespring.exceptions.OrderUnknownException;
import com.gav.binancespring.services.handlers.OrderMessageHandler;
import com.gav.binancespring.services.handlers.OrderWSHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderStreamService implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final BinanceSignatureService signatureService;
    private final OrderMessageHandler orderMessageHandler;
    private final WebSocketClient orderWebSocketClient;
    private final ScheduledExecutorService orderWebsocketScheduler;
    private final BinanceProperties properties;
    private volatile boolean running = true;
    private volatile boolean connecting;
    private volatile WebSocketSession session;
    private OrderWSHandler orderWSHandler;

    @Override
    public void run(ApplicationArguments args) {
        connect();
    }

    private void connect() {

        if (!running || connecting) {
            return;
        }
        connecting = true;
        orderWSHandler = new OrderWSHandler(
                signatureService,
                objectMapper,
                orderMessageHandler,
                this::onClosed,
                orderWebsocketScheduler,
                properties);
        orderWebSocketClient
                .execute(orderWSHandler, properties.orderUrl())
                .whenComplete((s, error) -> {
                    connecting = false;
                    if (error != null && running) {
                        log.warn("Сокет не открылся: " + error.getMessage());
                        log.info("Переподключаемся");
                        reconnect();
                        error.getCause();
                    } else {
                        this.session = s;
                        log.info("Сессия orders открыта");
                    }
                });
    }

    public OrderResult placeMarketOrder(String side, BigDecimal quantity) {
        if (session == null || !session.isOpen() || orderWSHandler == null) {
            throw new IllegalStateException("Ордер-сокет не подключен");
        }
        String requestId = UUID.randomUUID().toString();
        String clientOrderId = "b" + System.currentTimeMillis() + "-" +
                Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
        var filled = orderMessageHandler.await(requestId);
        Map<String, Object> params = new HashMap<>();
        params.put("side", side);
        params.put("quantity", quantity);
        params.put("symbol", "BTCUSDT");
        params.put("type", "MARKET");
        params.put("newOrderRespType", "FULL");
        params.put("newClientOrderId", clientOrderId);
        params.put("recvWindow", 10000);
        try {
            orderWSHandler.sendMessage(session, params, "order.place", requestId);
            return filled.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            orderMessageHandler.cancel(requestId);
//            throw new IllegalStateException("Нет ответа FILLED по ордеру " + requestId, e);
            throw new OrderUnknownException(clientOrderId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            orderMessageHandler.cancel(requestId);
            throw new IllegalStateException("Ожидание FILLED прервано", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(cause.getMessage(), cause);
        } catch (RuntimeException e) {
            orderMessageHandler.cancel(requestId);
            throw e;
        }
    }

    void onClosed() {
        this.session = null;
        if (running) {
            reconnect();
        }
    }

    private void reconnect() {
        long delay = properties.reconnectDelay();
        orderWebsocketScheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        orderWebsocketScheduler.shutdown();
    }
}
