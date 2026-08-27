package com.gav.binancespring.services;

import java.math.BigDecimal;

public interface TradeService {
    void trade(String type, BigDecimal requestPrice, BigDecimal bnbPrice);

    void analyse(BigDecimal btcPrice, BigDecimal bnbPrice);
}
