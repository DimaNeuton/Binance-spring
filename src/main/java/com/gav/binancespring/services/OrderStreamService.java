package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceTradeProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderStreamService implements ApplicationRunner {

    private Disposable connection;
    private final AtomicReference<WebSocketSession> activeSession = new AtomicReference<>();
    private final AtomicReference<Sinks.Many<String>> outboundRef = new AtomicReference<>();
    private final AtomicBoolean sessionReady = new AtomicBoolean(false);

    private final WebSocketClient webSocketClient;
    private final BinanceTradeProperties properties;
    private final OrderHandlerService orderHandlerService;
    private final BinanceSignatureService signatureService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        connection = connectWithReconnect().subscribe();
    }

    private Flux<Void> connectWithReconnect() {
        URI uri = URI.create(properties.url());

        return webSocketClient
                .execute(uri, this::handleSession)
                .doOnSubscribe(s -> log.info("[orders] Подключение к {}", uri))
                .doOnTerminate(() -> log.warn("[orders] Соединение с {} завершено", uri))
                .retryWhen(
                        Retry.backoff(properties.retryMaxAttempts(), properties.retryInitialDelay())
                                .maxBackoff(properties.retryMaxDelay())
                                .jitter(0.3)
                                .doBeforeRetry(retrySignal -> log.warn(
                                        "[orders] Повтор после ошибки. Попытка {}",
                                        retrySignal.totalRetries() + 1)))
                .repeatWhen(completed -> completed
                        .delayElements(properties.reconnectDelay())
                        .doOnNext(ignored -> log.info(
                                "[orders] Переподключение через {}",
                                properties.reconnectDelay())));
    }

    public void placeMarketOrder(String side, BigDecimal quantity) {
        String normalizedSide = side == null ? "" : side.toUpperCase();
        if (!normalizedSide.equals("BUY") && !normalizedSide.equals("SELL")) {
            throw new IllegalArgumentException("Сторона ордера должна быть BUY или SELL");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Объём должен быть больше 0");
        }

        Sinks.Many<String> outbound = outboundRef.get();
        if (outbound == null || !sessionReady.get()) {
            throw new IllegalStateException("Вебсокет ордеров ещё не готов");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("symbol", "BTCUSDT");
        params.put("side", normalizedSide);
        params.put("type", "MARKET");
        params.put("quantity", quantity.stripTrailingZeros().toPlainString());
        params.put("newOrderRespType", "FULL");
        params.put("timestamp", Instant.now().toEpochMilli());

        String json = buildRequest("order.place", params);
        Sinks.EmitResult result = outbound.tryEmitNext(json);
        if (result.isFailure()) {
            throw new IllegalStateException("Не удалось отправить order.place: " + result);
        }
        log.info("Отправлен order.place: {}", json);
    }

    private Mono<Void> handleSession(WebSocketSession session) {
        activeSession.set(session);
        sessionReady.set(false);
        String logonId = UUID.randomUUID().toString();
        AtomicBoolean subscribed = new AtomicBoolean(false);
        Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();
        outboundRef.set(outbound);

        Mono<Void> send = session.send(outbound.asFlux().map(session::textMessage));

        Mono<Void> receive = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .concatMap(message -> handleInbound(outbound, message, logonId, subscribed))
                .doOnError(e -> log.error("[orders] Ошибка в потоке сообщений", e))
                .doFinally(signal -> outbound.tryEmitComplete())
                .then();

        outbound.tryEmitNext(buildPingJson());
        outbound.tryEmitNext(buildSessionLogonJson(logonId));

        return Mono.when(send, receive)
                .doFinally(signal -> {
                    sessionReady.set(false);
                    outboundRef.compareAndSet(outbound, null);
                    activeSession.compareAndSet(session, null);
                });
    }

    private Mono<Void> handleInbound(
            Sinks.Many<String> outbound,
            String message,
            String logonId,
            AtomicBoolean subscribed) {
        try {
            orderHandlerService.handleMessage(message);
        } catch (Exception e) {
            log.error("[orders] Ошибка обработки сообщения", e);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(message);
        } catch (Exception e) {
            return Mono.empty();
        }

        if (!logonId.equals(root.path("id").asString(null))) {
            return Mono.empty();
        }

        int status = root.path("status").asInt();
        if (status != 200) {
            return Mono.error(new IllegalStateException(
                    "session.logon не удался: status=" + status + " " + root.path("error")));
        }

        if (!subscribed.compareAndSet(false, true)) {
            return Mono.empty();
        }

        Sinks.EmitResult result = outbound.tryEmitNext(buildUserDataSubscribeJson());
        if (result.isFailure()) {
            return Mono.error(new IllegalStateException(
                    "Не удалось отправить userDataStream.subscribe: " + result));
        }
        sessionReady.set(true);
        return Mono.empty();
    }

    /**
     * Проверка соединения. Binance отвечает с тем же id и status=200.
     */
    private String buildPingJson() {
        return buildRequest("ping", Map.of());
    }

    /**
     * Вход в WS-сессию по Ed25519. После успешного ответа apiKey и signature
     * в последующих SIGNED-запросах этой сессии не нужны.
     */
    private String buildSessionLogonJson(String id) {
        return buildRequest(id, "session.logon", signedParams(Map.of()));
    }

    /**
     * Подписка на события аккаунта после session.logon.
     */
    private String buildUserDataSubscribeJson() {
        return buildRequest("userDataStream.subscribe",
                Map.of("timestamp", Instant.now().toEpochMilli()));
    }

    private Map<String, Object> signedParams(Map<String, Object> businessParams) {
        Map<String, Object> params = new HashMap<>(businessParams);
        params.put("apiKey", properties.apiKey());
        params.put("timestamp", Instant.now().toEpochMilli());
        params.put("signature", signatureService.sign(params));
        return params;
    }

    private String buildRequest(String method, Map<String, Object> params) {
        return buildRequest(UUID.randomUUID().toString(), method, params);
    }

    private String buildRequest(String id, String method, Map<String, Object> params) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", id);
        request.put("method", method);
        if (!params.isEmpty()) {
            request.put("params", params);
        }

        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException e) {
            throw new IllegalStateException("Не удалось собрать JSON для метода " + method, e);
        }
    }

    @PreDestroy
    public void disconnect() {
        activeSession.set(null);
        if (connection != null) {
            connection.dispose();
            log.info("[orders] Websocket-соединение закрыто");
        }
    }
}
