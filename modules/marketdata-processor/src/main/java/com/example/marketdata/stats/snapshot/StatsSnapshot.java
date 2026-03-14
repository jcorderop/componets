package com.example.marketdata.stats.snapshot;

import java.util.Collections;
import java.util.Map;

/**
 * Flat immutable snapshot of statistics at a point in time.
 * Contains only metrics that were actually used/recorded.
 */
public record StatsSnapshot(
        String name,
        Map<String, Long> counters,
        Map<String, Long> gauges,
        Map<String, LatencySnapshot> latencies
) {
    public StatsSnapshot {
        counters = Collections.unmodifiableMap(counters);
        gauges = Collections.unmodifiableMap(gauges);
        latencies = Collections.unmodifiableMap(latencies);
    }

    /**
     * Immutable snapshot of latency metrics.
     */
    public record LatencySnapshot(
            long count,
            long totalMicros,
            long maxMicros
    ) {
        public double avgMicros() {
            return count > 0 ? (double) totalMicros / count : 0.0;
        }
    }
}
