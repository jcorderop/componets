package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessorStatsSinkLoggerService to ensure the logging format is safe
 * and that no formatting/runtime errors occur when publishing snapshots.
 */
class ProcessorStatsSinkLoggerServiceTest {

    private final ProcessorStatsSinkLoggerService service = new ProcessorStatsSinkLoggerService();

    private final Logger logger = (Logger) LoggerFactory.getLogger(ProcessorStatsSinkLoggerService.class);
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void publish_withEmptySnapshots_logsNothingAndDoesNotThrow() {
        listAppender.start();
        logger.addAppender(listAppender);

        // null
        assertDoesNotThrow(() -> service.publish(null));
        assertTrue(listAppender.list.isEmpty(), "No logs should be emitted for null snapshots");

        // empty list
        assertDoesNotThrow(() -> service.publish(List.of()));
        assertTrue(listAppender.list.isEmpty(), "No logs should be emitted for empty snapshots");
    }

    @Test
    void publish_withValidSnapshot_logsInfoWithExpectedContent() {
        listAppender.start();
        logger.addAppender(listAppender);

        // create a mock snapshot and stub the expected accessor calls
        ProcessorStatsSnapshot snapshot = mock(ProcessorStatsSnapshot.class);
        when(snapshot.processorName()).thenReturn("test-processor");
        when(snapshot.windowStartMillis()).thenReturn(1L);
        when(snapshot.windowEndMillis()).thenReturn(2L);
        when(snapshot.eventsEnqueued()).thenReturn(10L);
        when(snapshot.eventsProcessed()).thenReturn(9L);
        when(snapshot.eventsDropped()).thenReturn(1L);
        when(snapshot.minLatencyMillis()).thenReturn(5L);
        when(snapshot.maxLatencyMillis()).thenReturn(50L);
        when(snapshot.avgLatencyMillis()).thenReturn(12.5);

        // exercise
        assertDoesNotThrow(() -> service.publish(List.of(snapshot)));

        // assertions on logs
        assertFalse(listAppender.list.isEmpty(), "Expected at least one log event");
        ILoggingEvent evt = listAppender.list.get(0);
        assertEquals(ch.qos.logback.classic.Level.INFO, evt.getLevel(), "Expected INFO level log");
        String formatted = evt.getFormattedMessage();
        assertTrue(formatted.contains("Processor stats [test-processor]"), "Log should contain processor name");
        assertTrue(formatted.contains("window=1..2"), "Log should contain window boundaries");
        assertTrue(formatted.contains("enqueued=10"), "Log should contain enqueued count");
        assertTrue(formatted.contains("processed=9"), "Log should contain processed count");
        assertTrue(formatted.contains("dropped=1"), "Log should contain dropped count");
        assertTrue(formatted.contains("latency_ms"), "Log should contain latency details");

        // verify interactions with snapshot to ensure the code invoked the accessors
        verify(snapshot, atLeastOnce()).processorName();
        verify(snapshot, atLeastOnce()).windowStartMillis();
        verify(snapshot, atLeastOnce()).windowEndMillis();
        verify(snapshot, atLeastOnce()).eventsEnqueued();
        verify(snapshot, atLeastOnce()).eventsProcessed();
        verify(snapshot, atLeastOnce()).eventsDropped();
        verify(snapshot, atLeastOnce()).minLatencyMillis();
        verify(snapshot, atLeastOnce()).maxLatencyMillis();
        verify(snapshot, atLeastOnce()).avgLatencyMillis();
    }
}