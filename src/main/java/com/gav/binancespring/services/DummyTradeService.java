package com.gav.binancespring.services;

import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.LastBotCondition;
import com.gav.binancespring.entities.Transaction;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.repository.LastBotConditionRepository;
import com.gav.binancespring.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;

@Slf4j
@Service
//@RequiredArgsConstructor
public class DummyTradeService implements TradeService {

    private final int STATE_ID = 1;
    private ObjectMapper objectMapper;
    private TransactionRepository transactionRepository;
    private BalanceRepository balanceRepository;
    private LastBotConditionRepository lastBotConditionRepository;

    private final LastBotCondition lastBotCondition;
    private final Balance balance;

    private final BigDecimal commissionRatio = new BigDecimal("0.00075");
    private final BigDecimal volume = new BigDecimal("0.00006");
    private BigDecimal btcPrice = null;
    private BigDecimal tempBtcPrice = null;
    private BigDecimal bnbPrice = null;
    private Long requestTime;
    private BigDecimal requestPrice;
    private Long responseTime;
    private BigDecimal responseBtcPrice;
    private BigDecimal commission;

    @Autowired
    public DummyTradeService(ObjectMapper objectMapper,
                             TransactionRepository transactionRepository,
                             BalanceRepository balanceRepository,
                             LastBotConditionRepository lastBotConditionRepository) {
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
        this.lastBotConditionRepository = lastBotConditionRepository;
        this.lastBotCondition = lastBotConditionRepository.findById(STATE_ID).orElseThrow();
        this.balance = balanceRepository.findById(STATE_ID).orElseThrow();
        log.info("aaa");
        log.info("BTC = {} USDT = {} BNB = {}", balance.getBtc(), balance.getUsdt(), balance.getBnb());
        log.info("lastMaxPrice = {} lastMinPrice = {} upMoveDiffLevel = {} downMoveDiffLevel = {} count = {}",
                lastBotCondition.getLastMaxPrice(),
                lastBotCondition.getLastMinPrice(),
                lastBotCondition.getUpMoveDiffLevel(),
                lastBotCondition.getDownMoveDiffLevel(),
                lastBotCondition.getTransactionCount());
    }

    @Override
    public void handleMessage(String json) {

        try {
            JsonNode root = objectMapper.readTree(json);
            String streamType = root.path("stream").asString();
            JsonNode data = root.path("data");
            if (streamType.equals("btcusdt@bookTicker")) {

                btcPrice = new BigDecimal(data.get("b").asString());
                System.out.println("btc = " + btcPrice + "\t\t bnb = " + bnbPrice);

                if (btcPrice.compareTo(lastBotCondition.getLastMaxPrice().add(BigDecimal.valueOf(10))) > 0) {
                    trade("SELL");
                }

                if (btcPrice.compareTo(lastBotCondition.getLastMinPrice().subtract(BigDecimal.valueOf(10))) < 0) {
                    trade("BUY");
                }
            } else {
                bnbPrice = new BigDecimal(data.get("b").asString());
                System.out.println("btc = " + btcPrice + "\t\t bnb = " + bnbPrice);
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение: {}", json, e);
        }
    }

    @Override
    @Transactional
    public void trade(String type) {
        // отправляем запрос в WS ордеров
        requestTime = Instant.now().toEpochMilli();
        requestPrice = btcPrice;

        // получаем ответ
        responseTime = Instant.now().toEpochMilli();            // получаем из WS ордеров
        responseBtcPrice = btcPrice.add(BigDecimal.ONE);                               // получаем из WS ордеров

        commission = ((responseBtcPrice.multiply(volume)).multiply(commissionRatio)).divide(bnbPrice, 8, RoundingMode.HALF_DOWN);

        if (type.equals("SELL")) {
            balance.setBtc(balance.getBtc().subtract(volume));
            balance.setUsdt(balance.getUsdt().add(responseBtcPrice.multiply(volume)));
        } else {
            balance.setBtc(balance.getBtc().add(volume));
            balance.setUsdt(balance.getUsdt().subtract(responseBtcPrice.multiply(volume)));
        }
        balance.setBnb(balance.getBnb().subtract(commission));

        lastBotCondition.setLastMaxPrice(responseBtcPrice);
        lastBotCondition.setLastMinPrice(responseBtcPrice);
        lastBotCondition.setTransactionCount(lastBotCondition.getTransactionCount() + 1);

        // сохраняем транзакцию
        transactionRepository.save(Transaction.builder()
                .time(Instant.ofEpochMilli(responseTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .type(type)
                .price(responseBtcPrice)
                .volume(BigDecimal.valueOf(0.0001))
                .commission(commission)
                .btc(balance.getBtc())
                .usdt(balance.getUsdt())
                .bnb(balance.getBnb())
                .profit(BigDecimal.ZERO)        // рассчитывается на стороне БД
                .priceSlippage(responseBtcPrice.subtract(requestPrice).abs())
                .timeSlippage(responseTime - requestTime)
                .build());

        // сохраняем баланс
        balanceRepository.save(balance);

        // сохраняем параметры бота
        lastBotConditionRepository.save(lastBotCondition);

        System.out.println("Test " + type);
    }

//    @PostConstruct
//    void loadStateFromDB() {
//        lastBotCondition = lastBotConditionRepository.getById(STATE_ID);
//        balance = balanceRepository.getById(STATE_ID);
//    }
}
