package com.gav.binancespring.services;

import com.gav.binancespring.dto.CurrentStatus;
import com.gav.binancespring.entities.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CurrentStatusHolder {
    private final AtomicReference<CurrentStatus> ref = new AtomicReference<>(new CurrentStatus(
            null,
            0L,
            0L,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Instant.now(),
            null,
            null,
            null,
            new TreeMap<Long, BigDecimal>()));

    public CurrentStatus get() {
        return ref.get();
    }

    public void update(BigDecimal profit, Long closedTransactions, Long totalTransactions, BigDecimal tradingVolume,
                       BigDecimal btcReal, BigDecimal usdtReal, BigDecimal bnbReal,
                       BigDecimal btcDb, BigDecimal usdtDb, BigDecimal bnbDb, Transaction savedTransaction) {
        ref.updateAndGet(old -> {

            TreeMap<Long, BigDecimal> profits = new TreeMap<>(old.profits());
            profits.put(savedTransaction.getId(), profit);

            return new CurrentStatus(
                    profit,
                    closedTransactions,
                    totalTransactions,
                    tradingVolume,
                    btcReal,
                    usdtReal,
                    bnbReal,
                    btcDb,
                    usdtDb,
                    bnbDb,
                    old.uptime(),
                    savedTransaction,
                    old.transaction1(),
                    old.transaction2(),
                    profits);
        });
    }
}
