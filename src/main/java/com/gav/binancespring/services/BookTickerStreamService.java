package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceMarketProperties;
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
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@RequiredArgsConstructor
@Component
@Slf4j
public class BookTickerStreamService implements ApplicationRunner {
    private Disposable connection;

    private final WebSocketClient webSocketClient;
    private final BinanceMarketProperties properties;
    private final BookTickerHandlerService bookTickerHandlerService;
//    private final BinanceWebsocketService binanceWebsocketService;

    @Override
    public void run(ApplicationArguments args) {
        connection = connectWithReconnect().subscribe();
    }

    private Flux<Void> connectWithReconnect() {
        URI uri = URI.create(properties.url());

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
                .onBackpressureLatest()
                .concatMap(message ->
                                Mono.fromRunnable(() -> bookTickerHandlerService.handleMessage(message))
                                        .subscribeOn(Schedulers.boundedElastic()),
                        1)
                .doOnError(e -> log.error("Ошибка в потоке сообщений", e))
                .onErrorResume(e -> Mono.empty())
                .then();
//        session.receive()
//                .map(WebSocketMessage::getPayloadAsText)
//                .onBackpressureLatest()
//                .publishOn(Schedulers.boundedElastic(), 1)
//                .doOnNext(bookTickerHandlerService::handleMessage)
//                .then();
    }

    @PreDestroy
    public void disconnect() {
        if (connection != null) {
            connection.dispose();
            log.info("Websocket-соединение закрыто");
        }
    }
}
