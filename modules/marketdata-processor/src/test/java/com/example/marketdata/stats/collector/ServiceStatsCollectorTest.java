package com.example.marketdata.stats.collector;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceStatsCollectorTest {

    @Test
    void snapshotAndResetCollectsAllMetricTypesAndResetsWindow() {
        ServiceStatsCollector collector = new ServiceStatsCollector("custom-name");

        collector.counter("counter.metric").add(5);
        collector.gauge("gauge.metric").setMax(9);
        collector.gauge("gauge.metric").setMax(4);
        collector.latency("latency.metric").record(10);
        collector.latency("latency.metric").record(20);

        StatsSnapshot snapshot = collector.snapshotAndReset();

        assertEquals("custom-name", snapshot.name());
        assertEquals(5L, snapshot.counters().get("counter.metric"));
        assertEquals(9L, snapshot.gauges().get("gauge.metric"));
        assertEquals(15.0, snapshot.latencies().get("latency.metric").avg(), 1e-9);
        assertEquals(20.0, snapshot.latencies().get("latency.metric").max(), 1e-9);

        StatsSnapshot second = collector.snapshotAndReset();
        assertEquals(0L, second.counters().get("counter.metric"));
        assertEquals(0L, second.gauges().get("gauge.metric"));
        assertEquals(0.0, second.latencies().get("latency.metric").avg(), 1e-9);
        assertEquals(0.0, second.latencies().get("latency.metric").max(), 1e-9);
    }
}
