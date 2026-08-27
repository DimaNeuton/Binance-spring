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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class DummyTradeService implements TradeService {

    private final int STATE_ID = 1;
    private final Balance balance;
    private final LastBotCondition lastBotCondition;

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final LastBotConditionRepository lastBotConditionRepository;

    private final BigDecimal commissionRatio = new BigDecimal("0.00075");
    private final BigDecimal volume = new BigDecimal("0.00006");

    private Long responseTime;
    private BigDecimal responseBtcPrice;
    private BigDecimal commission;

    @Autowired
    public DummyTradeService(TransactionRepository transactionRepository,
                             BalanceRepository balanceRepository,
                             LastBotConditionRepository lastBotConditionRepository) {
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
        this.lastBotConditionRepository = lastBotConditionRepository;
        this.lastBotCondition = lastBotConditionRepository.findById(STATE_ID).orElseThrow();
        this.balance = balanceRepository.findById(STATE_ID).orElseThrow();
    }

    @Override
    public void trade(String type, BigDecimal requestPrice, BigDecimal bnbPrice) {
        // отправляем запрос в WS ордеров и ждем ответа
        Long requestTime = Instant.now().toEpochMilli();

        // получаем ответ
        responseTime = Instant.now().toEpochMilli();
        responseBtcPrice = requestPrice.add(BigDecimal.ONE);

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

        saveToDB(type, requestPrice, requestTime);

        System.out.println("Test " + type);
    }

    @Override
    public void analyse(BigDecimal btcPrice, BigDecimal bnbPrice) {
        if (btcPrice == null || bnbPrice == null) {
            return;
        }
        if (btcPrice.compareTo(lastBotCondition.getLastMaxPrice().add(BigDecimal.valueOf(80))) > 0) {
            trade("SELL", btcPrice, bnbPrice);
        }

        if (btcPrice.compareTo(lastBotCondition.getLastMinPrice().subtract(BigDecimal.valueOf(80))) < 0) {
            trade("BUY", btcPrice, bnbPrice);
        }
    }

    @Transactional
    public void saveToDB(String type, BigDecimal requestPrice, Long requestTime) {
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
    }
}
