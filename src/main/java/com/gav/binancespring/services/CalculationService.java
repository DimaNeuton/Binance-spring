package com.gav.binancespring.services;

import com.gav.binancespring.dto.CalcTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.TreeSet;

@Component
public class CalculationService {

    public BigDecimal getTotalCommission(TreeSet<CalcTransaction> sales, TreeSet<CalcTransaction> purchases) {
        return sales.isEmpty() && purchases.isEmpty() ? BigDecimal.ZERO : (sales.stream()
                .map(CalcTransaction::commission)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .add(purchases.stream()
                        .map(CalcTransaction::commission)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getTotaVolume(TreeSet<CalcTransaction> calcTransactions) {
        return (calcTransactions.stream()
                .map(CalcTransaction::volume)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
