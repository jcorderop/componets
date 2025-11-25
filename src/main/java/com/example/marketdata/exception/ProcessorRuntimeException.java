package com.example.marketdata.exception;

/**
 * Indicates a processor failure that should not be retried and must surface to operators.
 */
public class ProcessorRuntimeException extends RuntimeException {
    public ProcessorRuntimeException(String message) {
        super(message);
    }

    public ProcessorRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}

