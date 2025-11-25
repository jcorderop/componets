package com.example.marketdata.monitor.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the in-memory {@link ProcessorStatsRegistryImpl} implementation to ensure counters and
 * snapshots behave correctly across windows and multiple processors.
 */
class ProcessorStatsRegistryImplTest {

    private final ProcessorStatsRegistryImpl registry = new ProcessorStatsRegistryImpl();

    @Test
    void snapshotAndResetReturnsEmptyListWhenNoData() {
        // given

        // when
        List<ProcessorStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertNotNull(snapshots);
        assertTrue(snapshots.isEmpty());
    }

    @Test
    void recordEnqueueDropAndQueueSizeSingleProcessor() {
        // given
        String processor = "processor-1";

        registry.recordEnqueue(processor);
        registry.recordEnqueue(processor);
        registry.recordEnqueue(processor);
        registry.recordDrop(processor);
        registry.recordQueueSize(processor, 42);

        // when
        List<ProcessorStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertEquals(1, snapshots.size());

        ProcessorStatsSnapshot snapshot = snapshots.get(0);
        assertEquals(processor, snapshot.processorName());
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
        String processor = "latency-processor";

        registry.recordBatchProcessed(processor, 10, 100L);
        registry.recordBatchProcessed(processor, 5, 50L);
        registry.recordQueueSize(processor, 7);

        // when
        long beforeSnapshot = System.currentTimeMillis();
        List<ProcessorStatsSnapshot> snapshots = registry.snapshotAndReset();
        long afterSnapshot = System.currentTimeMillis();

        // then
        assertEquals(1, snapshots.size());
        ProcessorStatsSnapshot snapshot = snapshots.get(0);

        assertEquals(processor, snapshot.processorName());
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
        String processor = "reset-processor";

        registry.recordEnqueue(processor);
        registry.recordBatchProcessed(processor, 1, 10L);

        // when
        List<ProcessorStatsSnapshot> first = registry.snapshotAndReset();

        // then
        assertEquals(1, first.size());
        ProcessorStatsSnapshot s1 = first.get(0);
        assertEquals(1L, s1.eventsEnqueued());
        assertEquals(1L, s1.eventsProcessed());

        // given
        registry.recordEnqueue(processor);
        registry.recordBatchProcessed(processor, 2, 20L);

        // when
        List<ProcessorStatsSnapshot> second = registry.snapshotAndReset();

        // then
        assertEquals(1, second.size());
        ProcessorStatsSnapshot s2 = second.get(0);

        assertEquals(1L, s2.eventsEnqueued());
        assertEquals(2L, s2.eventsProcessed());
        assertEquals(20.0 / 2.0, s2.avgLatencyMillis(), 1e-6);
    }

    @Test
    void snapshotAndResetHandlesMultipleProcessors() {
        // given
        String c1 = "processor-A";
        String c2 = "processor-B";

        registry.recordEnqueue(c1);
        registry.recordEnqueue(c1);
        registry.recordBatchProcessed(c1, 1, 5L);

        registry.recordEnqueue(c2);
        registry.recordBatchProcessed(c2, 3, 15L);

        // when
        List<ProcessorStatsSnapshot> snapshots = registry.snapshotAndReset();

        // then
        assertEquals(2, snapshots.size());

        ProcessorStatsSnapshot s1 = snapshots.stream()
                .filter(s -> s.processorName().equals(c1))
                .findFirst()
                .orElseThrow();
        ProcessorStatsSnapshot s2 = snapshots.stream()
                .filter(s -> s.processorName().equals(c2))
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
