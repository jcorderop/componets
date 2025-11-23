package com.example.marketdata.monitor.consumer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Ensures {@link ConsumerStatsReporter} retrieves snapshots and forwards them to every sink.
 */
class ConsumerStatsReporterTest {

    @Test
    void publishStatsDelegatesSnapshotsToAllSinks() {
        ConsumerStatsRegistry registry = mock(ConsumerStatsRegistry.class);
        ConsumerStatsSink sink1 = mock(ConsumerStatsSink.class);
        ConsumerStatsSink sink2 = mock(ConsumerStatsSink.class);

        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("c1")
                .windowStartMillis(1L)
                .windowEndMillis(2L)
                .eventsEnqueued(3L)
                .eventsProcessed(4L)
                .eventsDropped(0L)
                .minLatencyMillis(5L)
                .maxLatencyMillis(6L)
                .avgLatencyMillis(1.5)
                .queueSizeAtSnapshot(7)
                .build();

        List<ConsumerStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ConsumerStatsReporter reporter = new ConsumerStatsReporter(registry, List.of(sink1, sink2));

        reporter.publishStats();

        verify(registry, times(1)).snapshotAndReset();
        verify(sink1, times(1)).publish(snapshots);
        verify(sink2, times(1)).publish(snapshots);
        verifyNoMoreInteractions(sink1, sink2);
    }
}
