package com.example.marketdata.monitor.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@link ProcessorStatsSnapshot} record wires each field as expected.
 */
class ProcessorStatsSnapshotTest {

    @Test
    void constructorCreatesSnapshotWithAllFields() {
        // given
        ProcessorStatsSnapshot snapshot = new ProcessorStatsSnapshot(
                "test-processor",
                10L,
                20L,
                1L,
                2L,
                3L,
                4L,
                5L,
                2.5,
                6
        );

        // when

        // then
        assertEquals("test-processor", snapshot.processorName());
        assertEquals(10L, snapshot.windowStartMillis());
        assertEquals(20L, snapshot.windowEndMillis());
        assertEquals(1L, snapshot.eventsEnqueued());
        assertEquals(2L, snapshot.eventsProcessed());
        assertEquals(3L, snapshot.eventsDropped());
        assertEquals(4L, snapshot.minLatencyMillis());
        assertEquals(5L, snapshot.maxLatencyMillis());
        assertEquals(2.5, snapshot.avgLatencyMillis(), 1e-6);
        assertEquals(6, snapshot.queueSizeAtSnapshot());
    }
}
