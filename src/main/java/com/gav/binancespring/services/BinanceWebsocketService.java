package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceMarketProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;

@RequiredArgsConstructor
@Component
@Slf4j
public class BinanceWebsocketService implements ApplicationRunner {
    private Disposable connection;

    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;
    private final BinanceMarketProperties properties;
    private final TradeService tradeService;

    @Override
    public void run(ApplicationArguments args) {
        connection = connectWithReconnect().subscribe();
    }

    private Flux<Void> connectWithReconnect() {
        URI uri = URI.create(properties.streamUrl());

        return webSocketClient
                .execute(uri, this::handleSession)
                .doOnSubscribe(s -> log.info("Подключение к {}", uri))
                .doOnTerminate(() -> log.warn("Соединение с {} завершено", uri))
                .retryWhen(
                        Retry.backoff(properties.retryMaxAttempts(), properties.retryInitialDelay())
                                .maxBackoff(properties.retryMaxDelay())
                                .jitter(0.3)
                                .doBeforeRetry(retrySignal -> log.warn("Повтор после ошибки. Попытка {}", retrySignal.totalRetries() + 1)))
                .repeatWhen(completed -> completed
                        .delayElements(properties.reconnectDelay())
                        .doOnNext(ignored -> log.info("Переподключение через {}", properties.reconnectDelay())));

    }

    private Mono<Void> handleSession(WebSocketSession session) {
        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .timeout(Duration.ofSeconds(10))
                .doOnNext(tradeService::handleMessage)
                .doOnError(e -> log.error("Ошибка в потоке сообщений", e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    //TODO это уйдет в трейд сервис
    private void handleMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (root.has("p")) {
                String price = root.get("p").asText();
            }
        }catch (Exception e){
            log.warn("Не удалось разобрать сообщение: {}", json, e);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (connection != null) {
            connection.dispose();
            log.info("Websocket-соединение закрыто");
        }
    }
}
