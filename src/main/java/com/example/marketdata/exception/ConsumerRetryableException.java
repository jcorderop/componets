package com.example.marketdata.exception;

/**
 * Signals that a consumer operation failed in a way that can be retried without losing data.
 */
public class ConsumerRetryableException extends RuntimeException {
    public ConsumerRetryableException(final String message) {
        super(message);
    }

    public ConsumerRetryableException(final String message, Throwable cause) {
        super(message, cause);
    }
}
