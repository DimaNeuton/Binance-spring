//package com.gav.binancespring.repository;
//
//import com.gav.binancespring.dto.TradeHistoryRow;
//import com.gav.binancespring.entities.Balance;
//import com.gav.binancespring.entities.LastBotCondition;
//import com.gav.binancespring.entities.Transaction;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.math.BigDecimal;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public class TradingRepository {
//    private JdbcTemplate jdbcTemplate;
//
//    public TradingRepository(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public Optional<Balance> getExpectedBalance() {
//        List<Balance> result = jdbcTemplate.query(
//                "select * from expected_balance",
//                this::mapToBalance);
//        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
//    }
//
//    public Optional<LastBotCondition> getLastBotCondition() {
//        List<LastBotCondition> result = jdbcTemplate.query(
//                "select * from last_bot_condition",
//                this::mapToLastBotCondition);
//        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
//    }
//
//    public List<TradeHistoryRow> getTradeHistory(int daysNumber) {
//        return jdbcTemplate.query(
//                """
//                        select * from transactions
//                        """,
//                this::mapToTradeHistoryRow);
//    }
//
//    public Transaction saveTransaction(Transaction transaction, LastBotCondition lastBotCondition, Balance balance) {
//        jdbcTemplate.update(
//                """
//                        update transactions set
//                                                time = ?,
//                                                type = ?,
//                                                price = ?,
//                                                volume = ?,
//                                                commission = ?,
//                                                btc = ?,
//                                                usdt = ?,
//                                                bnb = ?,
//                                                price_slippage = ?,
//                                                time_slippage = ?
//                        """,
//                transaction.getTime(),
//                transaction.getType(),
//                transaction.getPrice(),
//                transaction.getVolume(),
//                transaction.getCommission(),
//                transaction.getBtc(),
//                transaction.getUsdt(),
//                transaction.getBnb(),
//                transaction.getPriceSlippage(),
//                transaction.getTimeSlippage());
//        saveCondition(lastBotCondition);
//        saveBalance(balance);
//        return transaction;
//    }
//
//    public void saveCondition(LastBotCondition lastBotCondition) {
//        jdbcTemplate.update(
//                """
//                        update last_bot_conditions set
//                                                       last_max_price = ?,
//                                                       last_min_price = ?,
//                                                       up_move_diff_level = ?,
//                                                       down_move_diff_level = ?,
//                                                       transaction_ount = ?
//                        """,
//                lastBotCondition.getLastMaxPrice(),
//                lastBotCondition.getLastMinPrice(),
//                lastBotCondition.getUpMoveDiffLevel(),
//                lastBotCondition.getDownMoveDiffLevel(),
//                lastBotCondition.getTransactionCount()
//        );
//    }
//
//    public void saveBalance(Balance balance) {
//        jdbcTemplate.update(
//                """
//                        update balance set
//                                           btc = ?,
//                                           usdt = ?,
//                                           bnb = ?
//                        """,
//                balance.getBtc(),
//                balance.getUsdt(),
//                balance.getBnb());
//    }
//
//    private TradeHistoryRow mapToTradeHistoryRow(ResultSet resultSet, int i) throws SQLException {
//        return new TradeHistoryRow(
//                resultSet.getObject("id") == null ? null : resultSet.getLong("id"),
//                resultSet.getTimestamp("time") == null ? null : resultSet.getTimestamp("time").toLocalDateTime(),
//                resultSet.getString("type") == null ? null : resultSet.getString("type"),
//                resultSet.getBigDecimal("price") == null ? null : resultSet.getBigDecimal("price"),
//                resultSet.getBigDecimal("volume") == null ? null : resultSet.getBigDecimal("volume"),
//                resultSet.getBigDecimal("commission") == null ? null : resultSet.getBigDecimal("commission"),
//                resultSet.getBigDecimal("btc") == null ? null : resultSet.getBigDecimal("btc"),
//                resultSet.getBigDecimal("usdt") == null ? null : resultSet.getBigDecimal("usdt"),
//                resultSet.getBigDecimal("bnb") == null ? null : resultSet.getBigDecimal("bnb"),
//                resultSet.getBigDecimal("profit") == null ? null : resultSet.getBigDecimal("profit"),
//                resultSet.getBigDecimal("time_slippage") == null ? null : resultSet.getBigDecimal("time_slippage"),
//                resultSet.getBigDecimal("price_slippage") == null ? null : resultSet.getBigDecimal("price_slippage")
//        );
//    }
//
//    private LastBotCondition mapToLastBotCondition(ResultSet resultSet, int i) throws SQLException {
//        return new LastBotCondition(
//                resultSet.getBigDecimal("last_max_price"),
//                resultSet.getBigDecimal("Last_min_price"),
//                resultSet.getBigDecimal("up_move_diff_level"),
//                resultSet.getBigDecimal("down_move_diff_level"),
//                resultSet.getLong("transaction_count"));
//    }
//
//    private Balance mapToBalance(ResultSet resultSet, int i) throws SQLException {
//        return new Balance(
//                resultSet.getBigDecimal("btc"),
//                resultSet.getBigDecimal("usdt"),
//                resultSet.getBigDecimal("bnb"));
//    }
//}
