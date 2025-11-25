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

        ProcessorStatsSnapshot snapshot = new ProcessorStatsSnapshot(
                "logger-processor",
                1L,
                2L,
                1L,
                1L,
                0L,
                1L,
                1L,
                1.0,
                0
        );

        // when
        sink.publish(List.of(snapshot));

        // then
    }
}
