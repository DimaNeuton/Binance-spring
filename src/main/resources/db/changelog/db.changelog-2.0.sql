--liquibase formatted sql

-- changeset binance-db:1
INSERT INTO transactions (time, price, volume, type, commission,
                          btc, usdt, bnb, profit, price_slippage, time_slippage)
VALUES ('2026-07-12 10:15:05', 50000.50, 0.5, 'BUY', 0.001,
        0.5, 25000.25, 0.01, -1000, 0.001, 50);

INSERT INTO transactions (time, price, volume, type, commission,
                          btc, usdt, bnb, profit, price_slippage, time_slippage)
VALUES ('2026-07-12 14:45:03', 52000.00, 1.0, 'BUY', 0.0015,
        1.0, 52000.00, 0.02, -1000, 0.002, 120);

INSERT INTO last_bot_condition (last_max_price, last_min_price, up_move_diff_level, down_move_diff_level, transaction_count)
VALUES (53000, 52500, 53500, 52000, 2);

INSERT INTO balance (btc, usdt, bnb)
VALUES (1, 10000, 1);