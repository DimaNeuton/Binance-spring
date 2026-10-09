--liquibase formatted sql

-- changeset binance-db:reset-1

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;