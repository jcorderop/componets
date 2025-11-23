package com.example.marketdata.monitor.consumer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Ensures {@link ConsumerStatsReporter} retrieves snapshots and forwards them to every sink.
 */
class ConsumerStatsReporterTest {

    @Test
    void publishStatsHappyPathPublishesToAllSinks() throws Exception {
        ConsumerStatsRegistry registry = mock(ConsumerStatsRegistry.class);
        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ConsumerStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ConsumerStatsSink sink1 = mock(ConsumerStatsSink.class);
        ConsumerStatsSink sink2 = mock(ConsumerStatsSink.class);

        ConsumerStatsReporter reporter =
                new ConsumerStatsReporter(registry, List.of(sink1, sink2));

        reporter.publishStats();

        verify(sink1).publish(snapshots);
        verify(sink2).publish(snapshots);
    }

    @Test
    void publishStatsContinuesWhenOneSinkThrowsRuntimeException() throws Exception {
        ConsumerStatsRegistry registry = mock(ConsumerStatsRegistry.class);
        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ConsumerStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ConsumerStatsSink failingSink = mock(ConsumerStatsSink.class);
        ConsumerStatsSink succeedingSink = mock(ConsumerStatsSink.class);

        doThrow(new RuntimeException("boom"))
                .when(failingSink).publish(snapshots);

        ConsumerStatsReporter reporter =
                new ConsumerStatsReporter(registry, List.of(failingSink, succeedingSink));

        reporter.publishStats();

        verify(failingSink).publish(snapshots);
        verify(succeedingSink).publish(snapshots);
    }

    @Test
    void publishStatsStopsWhenCurrentThreadIsInterruptedDuringSink() throws Exception {
        ConsumerStatsRegistry registry = mock(ConsumerStatsRegistry.class);
        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ConsumerStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ConsumerStatsSink interruptedSink = mock(ConsumerStatsSink.class);
        ConsumerStatsSink neverCalledSink = mock(ConsumerStatsSink.class);

        // Simulate interruption while publishing
        doAnswer(invocation -> {
            // mark the current thread as interrupted
            Thread.currentThread().interrupt();
            // throw a runtime exception to enter the catch block
            throw new RuntimeException("stop");
        }).when(interruptedSink).publish(snapshots);

        ConsumerStatsReporter reporter =
                new ConsumerStatsReporter(registry, List.of(interruptedSink, neverCalledSink));

        reporter.publishStats();

        verify(interruptedSink).publish(snapshots);
        verifyNoInteractions(neverCalledSink);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void publishStatsStopsWhenSinkThrowsWrappedInterruptedException() throws Exception {
        ConsumerStatsRegistry registry = mock(ConsumerStatsRegistry.class);
        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ConsumerStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ConsumerStatsSink interruptedSink = mock(ConsumerStatsSink.class);
        ConsumerStatsSink neverCalledSink = mock(ConsumerStatsSink.class);

        // We cannot throw a checked InterruptedException directly (method doesn't declare it),
        // so we wrap it in a RuntimeException and let shouldStopOn(e.getCause()) handle it.
        doThrow(new RuntimeException(new InterruptedException("stop")))
                .when(interruptedSink).publish(snapshots);

        ConsumerStatsReporter reporter =
                new ConsumerStatsReporter(registry, List.of(interruptedSink, neverCalledSink));

        reporter.publishStats();

        verify(interruptedSink).publish(snapshots);
        verifyNoInteractions(neverCalledSink);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}