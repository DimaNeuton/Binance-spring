package com.gav.binancespring.services;

import java.math.BigDecimal;

public interface TradeService {
    void trade(String type, BigDecimal tickerPrice, Long tickerTime, BigDecimal bnbPrice);

    void analyse(BigDecimal btcSellPrice, BigDecimal btcBuyPrice, Long tickerTime, BigDecimal bnbPrice);
}
