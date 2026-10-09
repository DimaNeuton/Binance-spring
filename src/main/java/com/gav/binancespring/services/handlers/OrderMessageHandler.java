package com.gav.binancespring.services.handlers;

import com.gav.binancespring.dto.OrderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class OrderMessageHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CompletableFuture<OrderResult>> pending = new ConcurrentHashMap<>();

    public OrderMessageHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<OrderResult> await(String requestId) {
        CompletableFuture<OrderResult> future = new CompletableFuture<>();
        pending.put(requestId, future);
        return future;
    }

    public void cancel(String requestId) {
        CompletableFuture<OrderResult> future = pending.remove(requestId);
        if (future != null) {
            future.cancel(true);
        }
    }

    public void handleMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (root.has("event")) {
                log.info("User data event: {}", json);
                return;
            }

            if (!root.has("status")) {
                return;
            }

            String id = root.path("id").asString();
            int status = root.path("status").asInt();
            CompletableFuture<OrderResult> future = pending.get(id);

            if (status == 200) {
//                log.info("WS API ответ: {}", json);
                if (future == null) {
                    return;
                }
                JsonNode result = root.path("result");
                String orderStatus = result.path("status").asString();
                pending.remove(id);
                if ("FILLED".equals(orderStatus)) {
                    future.complete(toOrderResult(id, status, result));
                } else {
                    future.completeExceptionally(
                            new IllegalStateException("Ожидали FILLED, получили: " + orderStatus));
                }
            } else {
                log.warn("WS API ошибка: {}", json);
                if (future == null) {
                    return;
                }
                pending.remove(id);
                String error = root.path("error").path("msg").asString();
                if (error == null || error.isBlank()) {
                    error = "Ошибка ордера, HTTP-статус " + status;
                }
                future.completeExceptionally(new IllegalStateException(error));
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение ордер-стрима: {}", json, e);
        }
    }

    private OrderResult toOrderResult(String id, int status, JsonNode result) {
        BigDecimal executedQty = parseDecimal(result, "executedQty");
        BigDecimal quoteQty = parseDecimal(result, "cummulativeQuoteQty");
        BigDecimal price = parseDecimal(result, "price");
        BigDecimal commission = BigDecimal.ZERO;
        for (JsonNode fill : result.path("fills")) {
            commission = commission.add(parseDecimal(fill, "commission"));
        }
        if (executedQty != null && executedQty.signum() > 0
                && quoteQty != null && quoteQty.signum() > 0) {
            price = quoteQty.divide(executedQty, 8, RoundingMode.HALF_DOWN);
        }
        return new OrderResult(
                id,
                status,
                price,
                executedQty,
                commission,
                result.path("transactTime").asLong(),
                null);
    }

    private static BigDecimal parseDecimal(JsonNode node, String field) {
        String value = node.path(field).asString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }
}
