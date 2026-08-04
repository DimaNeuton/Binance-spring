package com.gav.binancespring.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TradeHistory {
    private List<TradeHistoryRow> rows = new ArrayList<>();

    public void addRow(TradeHistoryRow row) {
        rows.add(row);
    }
}
