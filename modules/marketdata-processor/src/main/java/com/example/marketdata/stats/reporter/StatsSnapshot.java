package com.example.marketdata.stats.reporter;

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
        counters = Map.copyOf(counters);
        gauges = Map.copyOf(gauges);
        latencies = Map.copyOf(latencies);
    }

    public record LatencySnapshot(
            double avg,
            long max
    ) {}
}
