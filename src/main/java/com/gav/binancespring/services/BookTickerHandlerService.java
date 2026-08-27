package com.gav.binancespring.services;

import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.LastBotCondition;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.repository.LastBotConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
@Slf4j
//@RequiredArgsConstructor
public class BookTickerHandlerService {
    private final ObjectMapper objectMapper;
    private String btcPriceString = null;
    private String bnbPriceString = null;
    private BigDecimal btcPrice = null;
    private BigDecimal bnbPrice = null;

    private final DummyTradeService dummyTradeService;

    @Autowired
    public BookTickerHandlerService(ObjectMapper objectMapper, DummyTradeService dummyTradeService) {
        this.objectMapper = objectMapper;
        this.dummyTradeService = dummyTradeService;
    }

    public void handleMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String streamType = root.path("stream").asString();
            JsonNode data = root.path("data");

            if (streamType.equals("btcusdt@bookTicker")) {
                if (!data.get("b").asString().equals(btcPriceString)) {
                    btcPriceString = data.get("b").asString();
                    btcPrice = new BigDecimal(btcPriceString);
                    System.out.println("btc = " + btcPrice + "\t\t bnb = " + bnbPrice);

                    dummyTradeService.analyse(btcPrice, bnbPrice);
                }
            } else {
                if (!data.get("b").asString().equals(bnbPriceString)) {
                    bnbPriceString = data.get("b").asString();
                    bnbPrice = new BigDecimal(bnbPriceString);
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение: {}", json, e);
        }
    }
}
