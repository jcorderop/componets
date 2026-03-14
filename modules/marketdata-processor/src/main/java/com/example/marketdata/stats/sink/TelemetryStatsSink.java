package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.Meter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes stats snapshots using OpenTelemetry metrics SDK.
 * Designed for telemetry/APM pipelines (for example APM -> Elastic).
 */
public class TelemetryStatsSink implements IStatsSink {

    private static final AttributeKey<String> SNAPSHOT_KEY = AttributeKey.stringKey("snapshot");

    private final String metricPrefix;
    private final Meter meter;

    private final ConcurrentMap<String, LongCounter> longCounters = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AtomicLong> longGaugeValues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicReference<Double>> doubleGaugeValues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObservableLongGauge> longGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObservableDoubleGauge> doubleGauges = new ConcurrentHashMap<>();

    public TelemetryStatsSink(String metricPrefix, OpenTelemetry openTelemetry) {
        this(metricPrefix, openTelemetry.getMeter("marketdata.stats.telemetry"));
    }

    TelemetryStatsSink(String metricPrefix, Meter meter) {
        this.metricPrefix = metricPrefix;
        this.meter = meter;
    }

    @Override
    public void publish(StatsSnapshot snapshot) {
        final String snapshotName = snapshot.name();
        final Attributes attrs = Attributes.of(SNAPSHOT_KEY, snapshotName);

        snapshot.counters().forEach((name, value) -> {
            String metricName = sanitize(metricPrefix + "_" + name + "_total");
            longCounters.computeIfAbsent(metricName, key ->
                    meter.counterBuilder(metricName).setDescription("Stats counter").build()
            ).add(value, attrs);
        });

        snapshot.gauges().forEach((name, value) -> {
            String metricName = sanitize(metricPrefix + "_" + name);
            updateLongGauge(metricName, snapshotName, value, attrs);
        });

        snapshot.latencies().forEach((name, latency) -> {
            String avgMetricName = sanitize(metricPrefix + "_" + name + "_avg_ms");
            updateDoubleGauge(avgMetricName, snapshotName, latency.avg(), attrs);

            String maxMetricName = sanitize(metricPrefix + "_" + name + "_max_ms");
            updateDoubleGauge(maxMetricName, snapshotName, latency.max(), attrs);
        });
    }

    private void updateLongGauge(String metricName, String snapshotName, long value, Attributes attrs) {
        longGaugeValues.computeIfAbsent(metricKey(metricName, snapshotName), key -> {
            AtomicLong holder = new AtomicLong(value);
            longGauges.computeIfAbsent(key, ignored -> meter.gaugeBuilder(metricName)
                    .ofLongs()
                    .setDescription("Stats gauge")
                    .buildWithCallback(measurement -> measurement.record(holder.get(), attrs)));
            return holder;
        }).set(value);
    }

    private void updateDoubleGauge(String metricName, String snapshotName, double value, Attributes attrs) {
        doubleGaugeValues.computeIfAbsent(metricKey(metricName, snapshotName), key -> {
            AtomicReference<Double> holder = new AtomicReference<>(value);
            doubleGauges.computeIfAbsent(key, ignored -> meter.gaugeBuilder(metricName)
                    .ofDoubles()
                    .setDescription("Stats latency gauge")
                    .buildWithCallback(measurement -> measurement.record(holder.get(), attrs)));
            return holder;
        }).set(value);
    }

    private String metricKey(String metricName, String snapshotName) {
        return metricName + "|" + snapshotName;
    }

    private String sanitize(final String name) {
        return name.toLowerCase().replace('.', '_');
    }
}
