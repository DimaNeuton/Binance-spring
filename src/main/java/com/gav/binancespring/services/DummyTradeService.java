package com.gav.binancespring.services;

import org.springframework.stereotype.Service;

@Service
public class DummyTradeService implements TradeService {
    @Override
    public void handleMessage(String message) {
        System.out.println("Test buy");
    }
}
