package com.example.marketdata.exception;

/**
 * Indicates a consumer failure that should not be retried and must surface to operators.
 */
public class ConsumerRuntimeException extends RuntimeException {
    public ConsumerRuntimeException(String message) {
        super(message);
    }

    public ConsumerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}

