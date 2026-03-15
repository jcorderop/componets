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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, flat statistics collector used by the market-data pipeline.
 * <p>
 * In Spring mode, the snapshot name is configured with the
 * {@code marketdata.stats.snapshot.name} property (default: {@code default_service}).
 * This name is included in each {@link StatsSnapshot} and is used by sinks for report labels.
 * </p>
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

    /**
     * Creates a collector with a snapshot name injected from Spring configuration.
     *
     * @param snapshotName value of {@code marketdata.stats.snapshot.name}
     */
    @Autowired
    public ServiceStatsCollector(@Value("${marketdata.stats.snapshot.name:default_service}") String snapshotName) {
        this.snapshotName = snapshotName;
    }

    /**
     * Gets or creates a counter metric.
     *
     * @param statsName metric name (for example, {@code consume.events})
     * @return counter metric instance for that name
     */
    public ICounterMetric counter(final String statsName) {
        validateMetricName(statsName);
        return counters.computeIfAbsent(statsName, k -> new AtomicCounterMetric());
    }

    /**
     * Gets or creates a gauge metric.
     *
     * @param statsName metric name (for example, {@code dispatch.zmq.queue_size})
     * @return gauge metric instance for that name
     */
    public IGaugeMetric gauge(final String statsName) {
        validateMetricName(statsName);
        return gauges.computeIfAbsent(statsName, k -> new AtomicGaugeMetric());
    }

    /**
     * Gets or creates a latency metric.
     *
     * @param statsName metric name (for example, {@code pipeline.latency_ms})
     * @return latency metric instance for that name
     */
    public ILatencyMetric latency(final String statsName) {
        validateMetricName(statsName);
        return latencies.computeIfAbsent(statsName, k -> new AtomicLatencyMetric());
    }

    /**
     * Captures a point-in-time snapshot and resets all metric windows atomically per metric.
     *
     * @return immutable snapshot with collector name, counters, gauges, and latency aggregates
     */
    @Override
    public StatsSnapshot snapshotAndReset() {
        final Map<String, Long> counterSnapshot = new HashMap<>();
        final Map<String, Long> gaugeSnapshot = new HashMap<>();
        final Map<String, StatsSnapshot.LatencySnapshot> latencySnapshot = new HashMap<>();

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

    private static void validateMetricName(String statsName) {
        if (!AllowedMetricNames.ALL.contains(statsName)) {
            throw new IllegalArgumentException("Unknown metric name: [" + statsName + "]");
        }
    }
}
