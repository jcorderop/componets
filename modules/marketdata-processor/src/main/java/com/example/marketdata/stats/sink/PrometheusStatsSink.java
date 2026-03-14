package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Publishes statistics snapshots in Prometheus text exposition format.
 * Converts a flat snapshot to Prometheus metric lines.
 */
@Slf4j
@RequiredArgsConstructor
public class PrometheusStatsSink implements IStatsSink {

    private final String metricPrefix;

    @Override
    public void publish(final StatsSnapshot snapshot) {
        final List<String> metrics = new ArrayList<>();
        collectMetrics(snapshot, metrics);

        if (metrics.isEmpty()) {
            log.info("=== Prometheus Metrics: {} (empty) ===", snapshot.name());
            return;
        }

        log.info("=== Prometheus Metrics: {} ===", snapshot.name());
        metrics.forEach(log::info);

        // Here you would typically push to Prometheus Pushgateway
        // or expose via HTTP endpoint for scraping
    }

    private void collectMetrics(final StatsSnapshot snapshot, final List<String> metrics) {
        final String snapshotLabel = escapeLabel(snapshot.name());

        snapshot.counters().forEach((name, value) -> {
            final String metricName = sanitize(metricPrefix + "_" + name + "_total");
            addMetricHeader(metrics, metricName, "counter");
            metrics.add(String.format("%s{snapshot=\"%s\"} %d", metricName, snapshotLabel, value));
        });

        snapshot.gauges().forEach((name, value) -> {
            final String metricName = sanitize(metricPrefix + "_" + name);
            addMetricHeader(metrics, metricName, "gauge");
            metrics.add(String.format("%s{snapshot=\"%s\"} %d", metricName, snapshotLabel, value));
        });

        snapshot.latencies().forEach((name, latency) -> {
            final String avgMetricName = sanitize(metricPrefix + "_" + name + "_avg_ms");
            addMetricHeader(metrics, avgMetricName, "gauge");
            metrics.add(String.format("%s{snapshot=\"%s\"} %.4f", avgMetricName, snapshotLabel, latency.avg()));

            final String maxMetricName = sanitize(metricPrefix + "_" + name + "_max_ms");
            addMetricHeader(metrics, maxMetricName, "gauge");
            metrics.add(String.format("%s{snapshot=\"%s\"} %.4f", maxMetricName, snapshotLabel, latency.max()));
        });
    }

    private void addMetricHeader(final List<String> metrics, final String metricName, final String metricType) {
        metrics.add("# TYPE " + metricName + " " + metricType);
    }

    private String sanitize(final String name) {
        return name.toLowerCase()
                .replace("-", "_")
                .replace(":", "_")
                .replace('.', '_')
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String escapeLabel(final String labelValue) {
        return labelValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
