package com.gav.binancespring.services.handlers;

import com.gav.binancespring.services.DummyTradeService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
//@RequiredArgsConstructor
public class BookTickerMessageHandler {
    private final ObjectMapper objectMapper;
    private String btcSellPriceString = null;
    private String btcBuyPriceString = null;
    private String bnbSellPriceString = null;
    private String bnbBuyPriceString = null;
    private BigDecimal btcSellPrice = null;
    private BigDecimal btcBuyPrice = null;
    private BigDecimal bnbSellPrice = null;
    private BigDecimal bnbBuyPrice = null;
    private BigDecimal bnbPrice = null;
    private Long tickerTime = null;

    private final DummyTradeService dummyTradeService;
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "book-ticker-processing");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public BookTickerMessageHandler(ObjectMapper objectMapper, DummyTradeService dummyTradeService) {
        this.objectMapper = objectMapper;
        this.dummyTradeService = dummyTradeService;
    }

    public void handleMessage(String json) {
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        try {
            processingExecutor.execute(() -> {
                try {
                    processMessage(json);
                } finally {
                    processing.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            processing.set(false);
        }
    }

    private void processMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String streamType = root.path("stream").asString();
            JsonNode data = root.path("data");

            if (streamType.equals("btcusdt@bookTicker")) {
                if (!data.get("b").asString().equals(btcSellPriceString) || !data.get("a").asString().equals(btcBuyPriceString)) {
                    btcSellPriceString = data.get("b").asString();
                    btcSellPrice = new BigDecimal(btcSellPriceString).setScale(2,  RoundingMode.FLOOR);
                    btcBuyPriceString = data.get("a").asString();
                    btcBuyPrice = new BigDecimal(btcBuyPriceString).setScale(2,  RoundingMode.FLOOR);
                    tickerTime = Instant.now().toEpochMilli();

//                    log.info("btcSell = " + btcSellPrice + "\t\tbtcBuy = " + btcBuyPrice + "\t\tbnb = " + bnbPrice);
                    dummyTradeService.analyse(btcSellPrice, btcBuyPrice, tickerTime, bnbPrice);
                }
            } else {
                if (!data.get("b").asString().equals(bnbSellPriceString) || !data.get("a").asString().equals(bnbBuyPriceString)) {
                    bnbSellPriceString = data.get("b").asString();
                    bnbSellPrice = new BigDecimal(bnbSellPriceString);
                    bnbBuyPriceString = data.get("a").asString();
                    bnbBuyPrice = new BigDecimal(bnbBuyPriceString);
                    bnbPrice = (bnbSellPrice.add(bnbBuyPrice)).divide(BigDecimal.valueOf(2), 2, RoundingMode.FLOOR);
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение: {} - {}", json, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        processingExecutor.shutdownNow();
    }
}
