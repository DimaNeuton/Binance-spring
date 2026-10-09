package com.gav.binancespring.exceptions;

public class OrderUnknownException extends RuntimeException {
    private final String clientOrderId;

    public OrderUnknownException(String clientOrderId, Throwable cause) {
        super("Судьба ордера неизвестна: " + clientOrderId, cause);
        this.clientOrderId = clientOrderId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }
}
