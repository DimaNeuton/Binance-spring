package com.gav.binancespring.database;

import com.gav.binancespring.dto.TradeHistoryRow;
import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.ExpectedTransaction;
import com.gav.binancespring.entities.LastBotCondition;
import com.gav.binancespring.entities.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TradingRepository {
    private JdbcTemplate jdbcTemplate;

    public TradingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Balance> getExpectedBalance() {
        List<Balance> result = jdbcTemplate.query(
                "select * from expected_balance",
                this::mapToBalance);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<LastBotCondition> getLastBotCondition() {
        List<LastBotCondition> result = jdbcTemplate.query(
                "select * from last_bot_condition",
                this::mapToLastBotCondition);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<TradeHistoryRow> getTradeHistory(int daysNumber) {
        return jdbcTemplate.query(
                """
                        select
                            e.expected_id,
                            e.expected_time,
                            e.expected_type,
                            e.expected_price,
                            e.expected_volume,
                            e.expected_commission,
                            e.expected_btc,
                            e.expected_usdt,
                            e.expected_bnb,
                            e.expected_profit,
                            t.id,
                            t.time,
                            t.type,
                            t.price,
                            t.volume,
                            t.commission,
                            t.btc,
                            t.usdt,
                            t.bnb,
                            t.time_slippage,
                            t.price_slippage,
                            t.profit
                            from expected_transactions e
                            left join transactions t on e.expected_id = t.id
                            order by e.expected_id
                        """,
                this::mapToTradeHistoryRow);
    }

    public Transaction saveExpectedTransaction(Transaction expectedTransaction, LastBotCondition lastBotCondition) {
        jdbcTemplate.update(
                """
                        insert into expected_transactions (
                                                           "expected_id",
                                                           "expected_time",
                                                           "expected_type",
                                                           "expected_price",
                                                           "expected_volume",
                                                           "expected_commission",
                                                           "expected_btc",
                                                           "expected_usdt",
                                                           "expected_bnb")
                        values(?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                lastBotCondition.getTransactionCount(),
                expectedTransaction.getTime(),
                expectedTransaction.getType(),
                expectedTransaction.getPrice(),
                expectedTransaction.getVolume(),
                expectedTransaction.getCommission(),
                expectedTransaction.getBtc(),
                expectedTransaction.getUsdt(),
                expectedTransaction.getBnb());
        jdbcTemplate.update(
                """
                        insert into transactions (
                                                  "id",
                                                  "time",
                                                  "type",
                                                  "price",
                                                  "volume",
                                                  "commission",
                                                  "btc",
                                                  "usdt",
                                                  "bnb")
                        values(?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                lastBotCondition.getTransactionCount(),
                LocalDateTime.now(),
                expectedTransaction.getType(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        return expectedTransaction;
    }

    public Transaction saveTransaction(Transaction transaction, LastBotCondition lastBotCondition) {
        jdbcTemplate.update(
                """
                        update transactions set
                                                time = ?,
                                                type = ?,
                                                price = ?,
                                                volume = ?,
                                                commission = ?,
                                                btc = ?,
                                                usdt = ?,
                                                bnb = ?
                        where id = ?
                        """,
                transaction.getTime(),
                transaction.getType(),
                transaction.getPrice(),
                transaction.getVolume(),
                transaction.getCommission(),
                transaction.getBtc(),
                transaction.getUsdt(),
                transaction.getBnb(),
                lastBotCondition.getTransactionCount());
        return transaction;
    }

    public void saveCondition(LastBotCondition lastBotCondition) {
        jdbcTemplate.update(
                """
                        update last_bot_conditions set
                                                       last_max_price = ?,
                                                       last_min_price = ?,
                                                       up_move_diff_level = ?,
                                                       down_move_diff_level = ?,
                                                       transaction_ount = ?
                        """,
                lastBotCondition.getLastMaxPrice(),
                lastBotCondition.getLastMinPrice(),
                lastBotCondition.getUpMoveDiffLevel(),
                lastBotCondition.getDownMoveDiffLevel(),
                lastBotCondition.getTransactionCount()
        );
    }

    private TradeHistoryRow mapToTradeHistoryRow(ResultSet resultSet, int i) throws SQLException {
        return new TradeHistoryRow(
                resultSet.getLong("expected_id"),
                resultSet.getTimestamp("expected_time").toLocalDateTime(),
                resultSet.getString("expected_type"),
                resultSet.getBigDecimal("expected_price"),
                resultSet.getBigDecimal("expected_volume"),
                resultSet.getBigDecimal("expected_commission"),
                resultSet.getBigDecimal("expected_btc"),
                resultSet.getBigDecimal("expected_usdt"),
                resultSet.getBigDecimal("expected_bnb"),
                resultSet.getBigDecimal("expected_profit"),
                resultSet.getObject("id") == null ? null : resultSet.getLong("id"),
                resultSet.getTimestamp("time") == null ? null : resultSet.getTimestamp("time").toLocalDateTime(),
                resultSet.getString("type") == null ? null : resultSet.getString("type"),
                resultSet.getBigDecimal("price") == null ? null : resultSet.getBigDecimal("price"),
                resultSet.getBigDecimal("volume") == null ? null : resultSet.getBigDecimal("volume"),
                resultSet.getBigDecimal("commission") == null ? null : resultSet.getBigDecimal("commission"),
                resultSet.getBigDecimal("btc") == null ? null : resultSet.getBigDecimal("btc"),
                resultSet.getBigDecimal("usdt") == null ? null : resultSet.getBigDecimal("usdt"),
                resultSet.getBigDecimal("bnb") == null ? null : resultSet.getBigDecimal("bnb"),
                resultSet.getBigDecimal("profit") == null ? null : resultSet.getBigDecimal("profit"),
                resultSet.getBigDecimal("time_slippage") == null ? null : resultSet.getBigDecimal("time_slippage"),
                resultSet.getBigDecimal("price_slippage") == null ? null : resultSet.getBigDecimal("price_slippage")
        );
    }

    private LastBotCondition mapToLastBotCondition(ResultSet resultSet, int i) throws SQLException {
        return new LastBotCondition(
                resultSet.getBigDecimal("last_max_price"),
                resultSet.getBigDecimal("Last_min_price"),
                resultSet.getBigDecimal("up_move_diff_level"),
                resultSet.getBigDecimal("down_move_diff_level"),
                resultSet.getLong("transaction_count"));
    }

    private Balance mapToBalance(ResultSet resultSet, int i) throws SQLException {
        return new Balance(
                resultSet.getBigDecimal("btc"),
                resultSet.getBigDecimal("usdt"),
                resultSet.getBigDecimal("bnb"));
    }
}
