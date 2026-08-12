--liquibase formatted sql

--changeset binance-db:1
create table if not exists transactions
(
    id             BIGSERIAL PRIMARY KEY,
    time           TIMESTAMP,
    price          DECIMAL,
    volume         DECIMAL,
    type           VARCHAR(5),
    commission     DECIMAL,
    btc            DECIMAl,
    usdt           DECIMAl,
    bnb            DECIMAl,
    profit         DECIMAl,
    price_slippage DECIMAL,
    time_slippage  BIGINT
);

create table if not exists balance
(
    id   INT PRIMARY KEY DEFAULT 1,
    btc  DECIMAL,
    usdt DECIMAL,
    bnb  DECIMAL
);

create table if not exists last_bot_condition
(
    id                   INT PRIMARY KEY DEFAULT 1,
    last_max_price       DECIMAL,
    last_min_price       DECIMAL,
    up_move_diff_level   DECIMAL,
    down_move_diff_level DECIMAL,
    transaction_count    BIGINT
);