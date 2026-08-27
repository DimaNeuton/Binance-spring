package com.gav.binancespring.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class OrderHandlerService {

    private final ObjectMapper objectMapper;

    public OrderHandlerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (root.has("event")) {
                log.info("User data event: {}", json);
                return;
            }

            if (root.has("status")) {
                int status = root.path("status").asInt();
                if (status == 200) {
                    log.info("WS API ответ: {}", json);
                } else {
                    log.warn("WS API ошибка: {}", json);
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение ордер-стрима: {}", json, e);
        }
    }
}
