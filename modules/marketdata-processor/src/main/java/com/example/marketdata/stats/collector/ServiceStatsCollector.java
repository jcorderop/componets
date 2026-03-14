package com.example.marketdata.stats.collector;

import com.example.marketdata.stats.metric.AtomicCounterMetric;
import com.example.marketdata.stats.metric.AtomicGaugeMetric;
import com.example.marketdata.stats.metric.AtomicLatencyMetric;
import com.example.marketdata.stats.metric.ICounterMetric;
import com.example.marketdata.stats.metric.IGaugeMetric;
import com.example.marketdata.stats.metric.ILatencyMetric;
import com.example.marketdata.stats.snapshot.StatsSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, flat statistics collector.
 * Use path-based keys like "consume.events", "forward.zmq.latency", etc.
 * Thread-safe Spring component for collecting statistics.
 */
@Component
public class ServiceStatsCollector implements IStatsCollector {

    private final String snapshotName;
    private final Map<String, ICounterMetric> counters = new ConcurrentHashMap<>();
    private final Map<String, IGaugeMetric> gauges = new ConcurrentHashMap<>();
    private final Map<String, ILatencyMetric> latencies = new ConcurrentHashMap<>();

    public ServiceStatsCollector() {
        this("service");
    }

    @Autowired
    public ServiceStatsCollector(@Value("${marketdata.stats.snapshot.name:service}") String snapshotName) {
        this.snapshotName = snapshotName;
    }

    /**
     * Get or create a counter metric.
     * @param path metric path (e.g., "consume.events", "forward.zmq.events")
     */
    public ICounterMetric counter(String path) {
        return counters.computeIfAbsent(path, k -> new AtomicCounterMetric());
    }

    /**
     * Get or create a gauge metric.
     * @param path metric path (e.g., "consume.queueSize", "forward.zmq.queueSize")
     */
    public IGaugeMetric gauge(String path) {
        return gauges.computeIfAbsent(path, k -> new AtomicGaugeMetric());
    }

    /**
     * Get or create a latency metric.
     * @param path metric path (e.g., "pipeline.latency", "forward.zmq.latency")
     */
    public ILatencyMetric latency(String path) {
        return latencies.computeIfAbsent(path, k -> new AtomicLatencyMetric());
    }

    @Override
    public StatsSnapshot snapshotAndReset() {
        Map<String, Long> counterSnapshot = new HashMap<>();
        Map<String, Long> gaugeSnapshot = new HashMap<>();
        Map<String, StatsSnapshot.LatencySnapshot> latencySnapshot = new HashMap<>();

        // Atomically snapshot and reset counters (thread-safe)
        counters.forEach((path, counter) -> {
            long value = counter.sumThenReset();
            if (value != 0) {
                counterSnapshot.put(path, value);
            }
        });

        // Atomically snapshot and reset gauges (thread-safe)
        gauges.forEach((path, gauge) -> {
            long value = gauge.getAndReset();
            if (value != 0) {
                gaugeSnapshot.put(path, value);
            }
        });

        // Atomically snapshot and reset latencies (thread-safe)
        latencies.forEach((path, latency) -> {
            ILatencyMetric.LatencyValues values = latency.snapshotAndReset();
            if (values.count() > 0) {
                latencySnapshot.put(path, new StatsSnapshot.LatencySnapshot(
                        values.count(),
                        values.total(),
                        values.max()
                ));
            }
        });

        return new StatsSnapshot(
                snapshotName,
                counterSnapshot,
                gaugeSnapshot,
                latencySnapshot
        );
    }
}
