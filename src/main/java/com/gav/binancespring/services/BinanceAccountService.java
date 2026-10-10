package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceProperties;
import com.gav.binancespring.dto.OrderStatusResponse;
import com.gav.binancespring.entities.Balance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.print.DocFlavor;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class BinanceAccountService {

    private final BinanceProperties properties;
    private final BinanceSignatureService signatureService;
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper;

    public Balance fetchWalletBalance() {
        long timestamp = System.currentTimeMillis();
        String query = "timestamp=" + timestamp;
        String signature = URLEncoder.encode(
                signatureService.sign(Map.of("timestamp", timestamp)),
                StandardCharsets.UTF_8);
        URI uri = URI.create(properties.restUrl() + "/api/v3/account?" + query + "&signature=" + signature);

        String body = webClient.get()
                .uri(uri)
                .header("X-MBX-APIKEY", properties.apiKey())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(httpBody -> Mono.error(
                                        new IllegalStateException("Binance " + response.statusCode() + ": " + httpBody))))
                .bodyToMono(String.class)
                .block();

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Пустой ответ Binance GET /api/v3/account");
        }
        return parseBalance(body);
    }

    public OrderStatusResponse queryOrderByClientId(String clientOrderId) {
        long timestamp = System.currentTimeMillis();
        String query = "symbol=BTCUSDT&origClientOrderId=" + clientOrderId + "&timestamp=" + timestamp;
        String signature = URLEncoder.encode(
                signatureService.sign(Map.of(
                        "symbol", "BTCUSDT",
                        "origClientOrderId", clientOrderId,
                        "timestamp", timestamp)), StandardCharsets.UTF_8);
        URI uri = URI.create(properties.restUrl() + "/api/v3/order?" + query + "&signature=" + signature);
        String body = webClient.get()
                .uri(uri)
                .header("X-MBX-APIKEY", properties.apiKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return objectMapper.readValue(body, OrderStatusResponse.class);
    }

    private Balance parseBalance(String json) {
        JsonNode root = objectMapper.readTree(json);
        BigDecimal btc = BigDecimal.ZERO;
        BigDecimal usdt = BigDecimal.ZERO;
        BigDecimal bnb = BigDecimal.ZERO;

        for (JsonNode balance : root.path("balances")) {
            String asset = balance.path("asset").asString();
            BigDecimal free = new BigDecimal(balance.path("free").asString());

            switch (asset) {
                case "BTC":
                    btc = free;
                    break;
                case "USDT":
                    usdt = free;
                    break;
                case "BNB":
                    bnb = free;
                    break;
            }
        }
        return Balance.builder()
                .id(null)
                .btc(btc)
                .usdt(usdt)
                .bnb(bnb).build();
    }

    public BigDecimal defineMinVolume(BigDecimal price) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.binance.com")
                        .path("/api/v3/exchangeInfo")
                        .queryParam("symbol", "BTCUSDT")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty exchangeInfo");
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode filters = root.path("symbols").path(0).path("filters");
        for (JsonNode filter : filters) {
            String filterType = filter.path("filterType").asString();
            if (filterType.equals("NOTIONAL")) {
                return filter.path("minNotional").decimalValue().divide(price, 5, RoundingMode.CEILING);
            }
        }
        throw new IllegalStateException("Notional filter found");
    }
}
