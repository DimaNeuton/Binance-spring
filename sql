create database binance_db;

create table expected_transactions
(
    id         BIGSERIAL PRIMARY KEY,
    time       DATE,
    price      DECIMAL,
    volume     DECIMAL,
    type       VARCHAR(5),
    commission DECIMAl,
    btc DECIMAl,
    usdt DECIMAl,
    bnb DECIMAl
);

create table expected_balance
(
    btc  DECIMAL,
    usdt DECIMAL,
    bnb  DECIMAL
);

create table transactions
(
    id             BIGSERIAL PRIMARY KEY,
    time           DATE,
    price          DECIMAL,
    volume         DECIMAL,
    type           VARCHAR(5),
    commission     DECIMAL,
    btc DECIMAl,
        usdt DECIMAl,
        bnb DECIMAl,
    price_slippage DECIMAL,
    time_slippage  DECIMAL
);

create table balance
(
    btc  DECIMAL,
    usdt DECIMAL,
    bnb  DECIMAL
);

create table last_bot_condition
(
    last_max_price       DECIMAL,
    Last_min_price       DECIMAL,
    up_move_diff_level   DECIMAL,
    down_move_diff_level DECIMAL
);