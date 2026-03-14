package com.example.marketdata.stats.reporter;

import com.example.marketdata.stats.collector.IStatsCollector;
import com.example.marketdata.stats.sink.IStatsSink;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsReporterTest {

    @Test
    void reportPublishesSnapshotToAllSinksEvenWhenOneFails() {
        IStatsCollector collector = mock(IStatsCollector.class);
        IStatsSink okSink = mock(IStatsSink.class);
        IStatsSink failingSink = mock(IStatsSink.class);

        StatsSnapshot snapshot = new StatsSnapshot("x", Map.of(), Map.of(), Map.of());
        when(collector.snapshotAndReset()).thenReturn(snapshot);
        doThrow(new RuntimeException("boom")).when(failingSink).publish(snapshot);

        StatsReporter reporter = new StatsReporter(collector, List.of(failingSink, okSink));

        assertDoesNotThrow(reporter::report);
        verify(collector, times(1)).snapshotAndReset();
        verify(failingSink, times(1)).publish(snapshot);
        verify(okSink, times(1)).publish(snapshot);
    }
}
