package com.example.marketdata.monitor.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerStatsSnapshotTest {

    @Test
    void builderCreatesSnapshotWithAllFields() {
        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("test-consumer")
                .windowStartMillis(10L)
                .windowEndMillis(20L)
                .eventsEnqueued(1L)
                .eventsProcessed(2L)
                .eventsDropped(3L)
                .minLatencyMillis(4L)
                .maxLatencyMillis(5L)
                .avgLatencyMillis(2.5)
                .queueSizeAtSnapshot(6)
                .build();

        assertEquals("test-consumer", snapshot.consumerName());
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
