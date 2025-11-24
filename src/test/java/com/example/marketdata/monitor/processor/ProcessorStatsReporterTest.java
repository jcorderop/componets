package com.example.marketdata.monitor.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Ensures {@link ProcessorStatsReporter} retrieves snapshots and forwards them to every sink.
 */
class ProcessorStatsReporterTest {

    @Test
    void publishStatsHappyPathPublishesToAllSinks() throws Exception {
        ProcessorStatsRegistry registry = mock(ProcessorStatsRegistry.class);
        ProcessorStatsSnapshot snapshot = ProcessorStatsSnapshot.builder()
                .processorName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ProcessorStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ProcessorStatsSink sink1 = mock(ProcessorStatsSink.class);
        ProcessorStatsSink sink2 = mock(ProcessorStatsSink.class);

        ProcessorStatsReporter reporter =
                new ProcessorStatsReporter(registry, List.of(sink1, sink2));

        reporter.publishStats();

        verify(sink1).publish(snapshots);
        verify(sink2).publish(snapshots);
    }

    @Test
    void publishStatsContinuesWhenOneSinkThrowsRuntimeException() throws Exception {
        ProcessorStatsRegistry registry = mock(ProcessorStatsRegistry.class);
        ProcessorStatsSnapshot snapshot = ProcessorStatsSnapshot.builder()
                .processorName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ProcessorStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ProcessorStatsSink failingSink = mock(ProcessorStatsSink.class);
        ProcessorStatsSink succeedingSink = mock(ProcessorStatsSink.class);

        doThrow(new RuntimeException("boom"))
                .when(failingSink).publish(snapshots);

        ProcessorStatsReporter reporter =
                new ProcessorStatsReporter(registry, List.of(failingSink, succeedingSink));

        reporter.publishStats();

        verify(failingSink).publish(snapshots);
        verify(succeedingSink).publish(snapshots);
    }

    @Test
    void publishStatsStopsWhenCurrentThreadIsInterruptedDuringSink() throws Exception {
        ProcessorStatsRegistry registry = mock(ProcessorStatsRegistry.class);
        ProcessorStatsSnapshot snapshot = ProcessorStatsSnapshot.builder()
                .processorName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ProcessorStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ProcessorStatsSink interruptedSink = mock(ProcessorStatsSink.class);
        ProcessorStatsSink neverCalledSink = mock(ProcessorStatsSink.class);

        // Simulate interruption while publishing
        doAnswer(invocation -> {
            // mark the current thread as interrupted
            Thread.currentThread().interrupt();
            // throw a runtime exception to enter the catch block
            throw new RuntimeException("stop");
        }).when(interruptedSink).publish(snapshots);

        ProcessorStatsReporter reporter =
                new ProcessorStatsReporter(registry, List.of(interruptedSink, neverCalledSink));

        reporter.publishStats();

        verify(interruptedSink).publish(snapshots);
        verifyNoInteractions(neverCalledSink);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void publishStatsStopsWhenSinkThrowsWrappedInterruptedException() throws Exception {
        ProcessorStatsRegistry registry = mock(ProcessorStatsRegistry.class);
        ProcessorStatsSnapshot snapshot = ProcessorStatsSnapshot.builder()
                .processorName("c1").windowStartMillis(1L).windowEndMillis(2L)
                .build();
        List<ProcessorStatsSnapshot> snapshots = List.of(snapshot);
        when(registry.snapshotAndReset()).thenReturn(snapshots);

        ProcessorStatsSink interruptedSink = mock(ProcessorStatsSink.class);
        ProcessorStatsSink neverCalledSink = mock(ProcessorStatsSink.class);

        // We cannot throw a checked InterruptedException directly (method doesn't declare it),
        // so we wrap it in a RuntimeException and let shouldStopOn(e.getCause()) handle it.
        doThrow(new RuntimeException(new InterruptedException("stop")))
                .when(interruptedSink).publish(snapshots);

        ProcessorStatsReporter reporter =
                new ProcessorStatsReporter(registry, List.of(interruptedSink, neverCalledSink));

        reporter.publishStats();

        verify(interruptedSink).publish(snapshots);
        verifyNoInteractions(neverCalledSink);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}