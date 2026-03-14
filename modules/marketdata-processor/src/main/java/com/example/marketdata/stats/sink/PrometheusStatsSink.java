package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.snapshot.StatsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Publishes statistics snapshots in Prometheus format.
 * Converts hierarchical structure to flat metric names with labels.
 */
@Slf4j
@RequiredArgsConstructor
public class PrometheusStatsSink implements IStatsSink {

    private final String metricPrefix;

    @Override
    public void publish(StatsSnapshot snapshot) {
        List<String> metrics = new ArrayList<>();
        collectMetrics(snapshot, "", metrics);

        log.info("=== Prometheus Metrics ===");
        metrics.forEach(log::info);

        // Here you would typically push to Prometheus Pushgateway
        // or expose via HTTP endpoint for scraping
    }

    private void collectMetrics(StatsSnapshot snapshot, String path, List<String> metrics) {
        String currentPath = path.isEmpty() ? snapshot.name() : path + "_" + snapshot.name();

        // Export counters
        snapshot.counters().forEach((name, value) -> {
            String metricName = metricPrefix + "_" + currentPath + "_" + name + "_total";
            metrics.add(String.format("%s %d", sanitize(metricName), value));
        });

        // Export gauges
        snapshot.gauges().forEach((name, value) -> {
            String metricName = metricPrefix + "_" + currentPath + "_" + name;
            metrics.add(String.format("%s %d", sanitize(metricName), value));
        });

        // Export latencies (as multiple metrics)
        snapshot.latencies().forEach((name, latency) -> {
            String baseName = metricPrefix + "_" + currentPath + "_" + name;
            metrics.add(String.format("%s_count %d", sanitize(baseName), latency.count()));
            metrics.add(String.format("%s_sum_micros %d", sanitize(baseName), latency.totalMicros()));
            metrics.add(String.format("%s_max_micros %d", sanitize(baseName), latency.maxMicros()));
        });

        // Recursively process children
        snapshot.children().forEach((name, child) ->
                collectMetrics(child, currentPath, metrics));
    }

    private String sanitize(String name) {
        return name.toLowerCase()
                .replace("-", "_")
                .replace(":", "_")
                .replaceAll("[^a-z0-9_]", "");
    }
}
