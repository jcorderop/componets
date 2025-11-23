package com.example.marketdata.exception;

public class ConsumerRetryableException extends RuntimeException {
    public ConsumerRetryableException(final String message) {
        super(message);
    }

    public ConsumerRetryableException(final String message, Throwable cause) {
        super(message, cause);
    }
}
