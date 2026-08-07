package com.gav.binancespring.services;

import com.gav.binancespring.entities.Transaction;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.repository.LastBotConditionRepository;
import com.gav.binancespring.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;

@Slf4j
@Service
public class DummyTradeService implements TradeService {

    private ObjectMapper objectMapper;
    private TransactionRepository transactionRepository;
    private BalanceRepository balanceRepository;
    private LastBotConditionRepository lastBotConditionRepository;
    private final BigDecimal commissionRatio = new BigDecimal("0.00075");
    private final BigDecimal volume = new BigDecimal("0.00006");
    private BigDecimal BtcPrice = null;
    private BigDecimal BnbPrice = null;
    private Long requestTime;
    private BigDecimal requestPrice;
    private Long responseTime;
    private BigDecimal responseBtcPrice;
    private BigDecimal commission;

    @Override
    public void handleMessage(String json) {

        try {
            JsonNode root = objectMapper.readTree(json);

            if(root.get("stream").equals("btcusdt@trade")) {
                if (root.has("p")) {
                    BtcPrice = new BigDecimal(root.get("p").asText());
                    BnbPrice = new BigDecimal(root.get("p").asText());                      // ????????????
                }

                if (BtcPrice > lastBotCondition.getLastMaxPice().add(BigDecimal.TEN)) {
                    trade("SELL");
                }

                if(BtcPrice < lastBotCondition.getLastMaxPice().subtract(BigDecimal.TEN)) {
                    trade("BUY");
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение: {}", json, e);
        }
    }

    @Override
    public void trade(String type) {
        // отправляем запрос в WS ордеров
        requestTime = Instant.now().toEpochMilli();
        requestPrice = BtcPrice;

        // получаем ответ
        responseTime = Instant.now().toEpochMilli();            // получаем из WS ордеров
        responseBtcPrice = price;                               // получаем из WS ордеров

        commission = ((responseBtcPrice.multiply(volume)).multiply(commissionRatio)).divide(BnbPrice, 8, RoundingMode.HALF_DOWN);

        if (type.equals("SELL")) {
            balance.setBtc(balance.btc.subtract(volume));
            balance.setUsdt(balance.usdt.add(responseBtcPrice.multiply(volume)));
        } else {
            balance.setBtc(balance.btc.add(volume));
            balance.setUsdt(balance.usdt.subtract(responseBtcPrice.multiply(volume)));
        }
        balance.setBnb(balance.bnb.subtract(commission));

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
}
