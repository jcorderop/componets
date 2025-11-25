package com.example.marketdata.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProcessorExceptionsTest {

    @Test
    void retryableExceptionRetainsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("retry cause");
        ProcessorRetryableException exception = new ProcessorRetryableException("retry message", cause);

        assertEquals("retry message", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void runtimeExceptionRetainsMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("runtime cause");
        ProcessorRuntimeException exception = new ProcessorRuntimeException("runtime message", cause);

        assertEquals("runtime message", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
