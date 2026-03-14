package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes statistics snapshots to Micrometer so they can be scraped by Prometheus.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "stats.sink.prometheus", name = "enabled", havingValue = "true")
public class PrometheusStatsSinkService implements IStatsSink {

    private static final String TAG_SNAPSHOT = "snapshot";

    private final String metricPrefix;
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> gaugeHolders = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicReference<Double>> doubleGaugeHolders = new ConcurrentHashMap<>();

    public PrometheusStatsSinkService(
            @Value("${stats.sink.prometheus.metric-prefix:marketdata_stats}") String metricPrefix,
            MeterRegistry meterRegistry) {
        this.metricPrefix = metricPrefix;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(final StatsSnapshot snapshot) {
        final String snapshotName = snapshot.name();
        final Tags tags = Tags.of(TAG_SNAPSHOT, snapshotName);

        snapshot.counters().forEach((name, value) -> {
            final String metricName = sanitize(metricPrefix + "_" + name + "_total");
            meterRegistry.counter(metricName, tags).increment(value);
        });

        snapshot.gauges().forEach((name, value) -> {
            final String metricName = sanitize(metricPrefix + "_" + name);
            updateLongGauge(metricName, snapshotName, tags, value);
        });

        snapshot.latencies().forEach((name, latency) -> {
            final String avgMetricName = sanitize(metricPrefix + "_" + name + "_avg_ms");
            updateDoubleGauge(avgMetricName, snapshotName, tags, latency.avg());

            final String maxMetricName = sanitize(metricPrefix + "_" + name + "_max_ms");
            updateDoubleGauge(maxMetricName, snapshotName, tags, latency.max());
        });

        if (snapshot.counters().isEmpty() && snapshot.gauges().isEmpty() && snapshot.latencies().isEmpty()) {
            log.info("PrometheusStatsSinkService received empty snapshot {}", snapshotName);
        }
    }

    private void updateLongGauge(String metricName, String snapshotName, Tags tags, long value) {
        gaugeHolders.computeIfAbsent(metricKey(metricName, snapshotName), key -> {
            AtomicLong holder = new AtomicLong(value);
            Gauge.builder(metricName, holder, AtomicLong::get)
                    .tags(tags)
                    .register(meterRegistry);
            return holder;
        }).set(value);
    }

    private void updateDoubleGauge(String metricName, String snapshotName, Tags tags, double value) {
        doubleGaugeHolders.computeIfAbsent(metricKey(metricName, snapshotName), key -> {
            AtomicReference<Double> holder = new AtomicReference<>(value);
            Gauge.builder(metricName, holder, AtomicReference::get)
                    .tags(tags)
                    .register(meterRegistry);
            return holder;
        }).set(value);
    }

    private String metricKey(String metricName, String snapshotName) {
        return metricName + "|" + snapshotName;
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
}
