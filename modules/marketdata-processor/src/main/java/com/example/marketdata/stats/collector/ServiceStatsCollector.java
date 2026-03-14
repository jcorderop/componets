package com.example.marketdata.stats.collector;

import com.example.marketdata.stats.metric.AtomicCounterMetric;
import com.example.marketdata.stats.metric.AtomicGaugeMetric;
import com.example.marketdata.stats.metric.AtomicLatencyMetric;
import com.example.marketdata.stats.metric.ICounterMetric;
import com.example.marketdata.stats.metric.IGaugeMetric;
import com.example.marketdata.stats.metric.ILatencyMetric;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, flat statistics collector.
 * Use statsName-based keys like "consume.events", "forward.zmq.latency", etc.
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
    public ServiceStatsCollector(@Value("${marketdata.stats.snapshot.name:default_service}") String snapshotName) {
        this.snapshotName = snapshotName;
    }

    /**
     * Get or create a counter metric.
     * @param statsName metric statsName (e.g., "consume.events", "forward.zmq.events")
     */
    public ICounterMetric counter(final String statsName) {
        return counters.computeIfAbsent(statsName, k -> new AtomicCounterMetric());
    }

    /**
     * Get or create a gauge metric.
     * @param statsName metric statsName (e.g., "consume.queueSize", "forward.zmq.queueSize")
     */
    public IGaugeMetric gauge(final String statsName) {
        return gauges.computeIfAbsent(statsName, k -> new AtomicGaugeMetric());
    }

    /**
     * Get or create a latency metric.
     * @param statsName metric statsName (e.g., "pipeline.latency", "forward.zmq.latency")
     */
    public ILatencyMetric latency(final String statsName) {
        return latencies.computeIfAbsent(statsName, k -> new AtomicLatencyMetric());
    }

    @Override
    public StatsSnapshot snapshotAndReset() {
        final Map<String, Long> counterSnapshot = new ConcurrentHashMap<>();
        final Map<String, Long> gaugeSnapshot = new ConcurrentHashMap<>();
        final Map<String, StatsSnapshot.LatencySnapshot> latencySnapshot = new ConcurrentHashMap<>();

        // Atomically snapshot and reset counters (thread-safe)
        counters.forEach((statsName, counter) -> 
                counterSnapshot.put(statsName, counter.sumThenReset()));

        // Atomically snapshot and reset gauges (thread-safe)
        gauges.forEach((statsName, gauge) -> 
                gaugeSnapshot.put(statsName, gauge.getAndReset()));

        // Atomically snapshot and reset latencies (thread-safe)
        latencies.forEach((statsName, latency) -> {
                final ILatencyMetric.LatencyValues latencyValues = latency.snapshotAndReset();
                latencySnapshot.put(statsName, new StatsSnapshot.LatencySnapshot(latencyValues.avg(), latencyValues.max()));
            });

        return new StatsSnapshot(
                snapshotName,
                counterSnapshot,
                gaugeSnapshot,
                latencySnapshot
        );
    }
}
