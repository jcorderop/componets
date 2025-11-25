package com.example.marketdata.exception;

/**
 * Signals that a processor operation failed in a way that can be retried without losing data.
 */
public class ProcessorRetryableException extends RuntimeException {
    public ProcessorRetryableException(final String message) {
        super(message);
    }

    public ProcessorRetryableException(final String message, Throwable cause) {
        super(message, cause);
    }
}
