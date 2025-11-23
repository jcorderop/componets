package com.example.marketdata.monitor.consumer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the in-memory {@link ConsumerStatsRegistryImpl} implementation to ensure counters and
 * snapshots behave correctly across windows and multiple consumers.
 */
class ConsumerStatsRegistryImplTest {

    private final ConsumerStatsRegistryImpl registry = new ConsumerStatsRegistryImpl();

    @Test
    void snapshotAndResetReturnsEmptyListWhenNoData() {
        // given

        // when
        List<ConsumerStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertNotNull(snapshots);
        assertTrue(snapshots.isEmpty());
    }

    @Test
    void recordEnqueueDropAndQueueSizeSingleConsumer() {
        // given
        String consumer = "consumer-1";

        registry.recordEnqueue(consumer);
        registry.recordEnqueue(consumer);
        registry.recordEnqueue(consumer);
        registry.recordDrop(consumer);
        registry.recordQueueSize(consumer, 42);

        // when
        List<ConsumerStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertEquals(1, snapshots.size());

        ConsumerStatsSnapshot snapshot = snapshots.get(0);
        assertEquals(consumer, snapshot.consumerName());
        assertEquals(3L, snapshot.eventsEnqueued());
        assertEquals(0L, snapshot.eventsProcessed());
        assertEquals(1L, snapshot.eventsDropped());
        assertEquals(42, snapshot.queueSizeAtSnapshot());
        assertEquals(0L, snapshot.minLatencyMillis());
        assertEquals(0L, snapshot.maxLatencyMillis());
        assertEquals(0.0, snapshot.avgLatencyMillis(), 1e-6);
    }

    @Test
    void recordBatchProcessedUpdatesCountersAndLatency() {
        // given
        String consumer = "latency-consumer";

        registry.recordBatchProcessed(consumer, 10, 100L);
        registry.recordBatchProcessed(consumer, 5, 50L);
        registry.recordQueueSize(consumer, 7);

        // when
        long beforeSnapshot = System.currentTimeMillis();
        List<ConsumerStatsSnapshot> snapshots = registry.snapshotAndReset();
        long afterSnapshot = System.currentTimeMillis();

        // then
        assertEquals(1, snapshots.size());
        ConsumerStatsSnapshot snapshot = snapshots.get(0);

        assertEquals(consumer, snapshot.consumerName());
        assertEquals(15L, snapshot.eventsProcessed());
        assertEquals(0L, snapshot.eventsDropped());
        assertEquals(0L, snapshot.eventsEnqueued());

        assertEquals(50L, snapshot.minLatencyMillis());
        assertEquals(100L, snapshot.maxLatencyMillis());
        assertEquals(10.0, snapshot.avgLatencyMillis(), 1e-6);

        assertEquals(7, snapshot.queueSizeAtSnapshot());

        assertTrue(snapshot.windowStartMillis() <= snapshot.windowEndMillis());
        assertTrue(snapshot.windowEndMillis() >= beforeSnapshot);
        assertTrue(snapshot.windowEndMillis() <= afterSnapshot);
    }

    @Test
    void snapshotAndResetResetsBucketsBetweenWindows() {
        // given
        String consumer = "reset-consumer";

        registry.recordEnqueue(consumer);
        registry.recordBatchProcessed(consumer, 1, 10L);

        // when
        List<ConsumerStatsSnapshot> first = registry.snapshotAndReset();

        // then
        assertEquals(1, first.size());
        ConsumerStatsSnapshot s1 = first.get(0);
        assertEquals(1L, s1.eventsEnqueued());
        assertEquals(1L, s1.eventsProcessed());

        // given
        registry.recordEnqueue(consumer);
        registry.recordBatchProcessed(consumer, 2, 20L);

        // when
        List<ConsumerStatsSnapshot> second = registry.snapshotAndReset();

        // then
        assertEquals(1, second.size());
        ConsumerStatsSnapshot s2 = second.get(0);

        assertEquals(1L, s2.eventsEnqueued());
        assertEquals(2L, s2.eventsProcessed());
        assertEquals(20.0 / 2.0, s2.avgLatencyMillis(), 1e-6);
    }

    @Test
    void snapshotAndResetHandlesMultipleConsumers() {
        // given
        String c1 = "consumer-A";
        String c2 = "consumer-B";

        registry.recordEnqueue(c1);
        registry.recordEnqueue(c1);
        registry.recordBatchProcessed(c1, 1, 5L);

        registry.recordEnqueue(c2);
        registry.recordBatchProcessed(c2, 3, 15L);

        // when
        List<ConsumerStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertEquals(2, snapshots.size());

        ConsumerStatsSnapshot s1 = snapshots.stream()
                .filter(s -> s.consumerName().equals(c1))
                .findFirst()
                .orElseThrow();
        ConsumerStatsSnapshot s2 = snapshots.stream()
                .filter(s -> s.consumerName().equals(c2))
                .findFirst()
                .orElseThrow();

        assertEquals(2L, s1.eventsEnqueued());
        assertEquals(1L, s1.eventsProcessed());
        assertEquals(5L, s1.minLatencyMillis());
        assertEquals(5L, s1.maxLatencyMillis());

        assertEquals(1L, s2.eventsEnqueued());
        assertEquals(3L, s2.eventsProcessed());
        assertEquals(15L, s2.minLatencyMillis());
        assertEquals(15L, s2.maxLatencyMillis());
    }
}
