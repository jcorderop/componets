package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Validates that the logging sink safely handles snapshots without throwing exceptions.
 */
class ProcessorStatsSinkLoggerServiceTest {

    @Test
    void publishDoesNotThrowAndLogsSnapshots() {
        // given
        ProcessorStatsSinkLoggerService sink = new ProcessorStatsSinkLoggerService();

        ProcessorStatsSnapshot snapshot = ProcessorStatsSnapshot.builder()
                .processorName("logger-processor")
                .windowStartMillis(1L)
                .windowEndMillis(2L)
                .eventsEnqueued(1L)
                .eventsProcessed(1L)
                .eventsDropped(0L)
                .minLatencyMillis(1L)
                .maxLatencyMillis(1L)
                .avgLatencyMillis(1.0)
                .queueSizeAtSnapshot(0)
                .build();

        // when
        sink.publish(List.of(snapshot));

        // then
    }
}
