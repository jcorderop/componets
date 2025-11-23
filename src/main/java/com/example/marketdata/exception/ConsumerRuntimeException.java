package com.example.marketdata.exception;

public class ConsumerRuntimeException extends RuntimeException {
    public ConsumerRuntimeException(String message) {
        super(message);
    }

    public ConsumerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}

