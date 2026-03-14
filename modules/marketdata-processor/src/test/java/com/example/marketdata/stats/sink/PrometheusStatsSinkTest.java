package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrometheusStatsSinkTest {

    @Test
    void publishExportsCounterGaugeAndLatencyMetrics() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PrometheusStatsSink sink = new PrometheusStatsSink("market-data", registry);

        sink.publish(new StatsSnapshot(
                "snap\"name",
                Map.of("counter.one", 2L),
                Map.of("gauge-two", 3L),
                Map.of("lat.one", new StatsSnapshot.LatencySnapshot(1.25, 4.5))
        ));

        assertEquals(2.0, counterValue(registry, "market_data_counter_one_total", "snap\"name"));
        assertEquals(3.0, gaugeValue(registry, "market_data_gauge_two", "snap\"name"));
        assertEquals(1.25, gaugeValue(registry, "market_data_lat_one_avg_ms", "snap\"name"));
        assertEquals(4.5, gaugeValue(registry, "market_data_lat_one_max_ms", "snap\"name"));
    }

    @Test
    void publishAccumulatesCountersAndUpdatesGaugesPerSnapshot() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PrometheusStatsSink sink = new PrometheusStatsSink("marketdata", registry);

        sink.publish(new StatsSnapshot(
                "snap-a",
                Map.of("events", 5L),
                Map.of("queue.size", 7L),
                Map.of("proc", new StatsSnapshot.LatencySnapshot(1.0, 2.0))
        ));

        sink.publish(new StatsSnapshot(
                "snap-a",
                Map.of("events", 3L),
                Map.of("queue.size", 4L),
                Map.of("proc", new StatsSnapshot.LatencySnapshot(2.5, 9.0))
        ));

        sink.publish(new StatsSnapshot(
                "snap-b",
                Map.of("events", 2L),
                Map.of("queue.size", 11L),
                Map.of("proc", new StatsSnapshot.LatencySnapshot(4.0, 10.0))
        ));

        assertEquals(8.0, counterValue(registry, "marketdata_events_total", "snap-a"));
        assertEquals(2.0, counterValue(registry, "marketdata_events_total", "snap-b"));
        assertEquals(4.0, gaugeValue(registry, "marketdata_queue_size", "snap-a"));
        assertEquals(11.0, gaugeValue(registry, "marketdata_queue_size", "snap-b"));
        assertEquals(2.5, gaugeValue(registry, "marketdata_proc_avg_ms", "snap-a"));
        assertEquals(9.0, gaugeValue(registry, "marketdata_proc_max_ms", "snap-a"));
    }

    private double counterValue(MeterRegistry registry, String name, String snapshotName) {
        return registry.get(name)
                .tag("snapshot", snapshotName)
                .counter()
                .count();
    }

    private double gaugeValue(MeterRegistry registry, String name, String snapshotName) {
        Double value = registry.get(name)
                .tag("snapshot", snapshotName)
                .gauge()
                .value();
        return value == null ? 0.0 : value;
    }
}
