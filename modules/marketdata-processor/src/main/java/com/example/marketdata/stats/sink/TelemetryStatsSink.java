package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class TelemetryStatsSink implements IStatsSink {

    private static final AttributeKey<String> SNAPSHOT_KEY = AttributeKey.stringKey("snapshot");

    private final String metricPrefix;
    private final Meter meter;

    private final ConcurrentMap<String, LongCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<Attributes, AtomicReference<Double>>> gaugeValues =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObservableDoubleGauge> registeredGauges =
            new ConcurrentHashMap<>();

    public TelemetryStatsSink(String metricPrefix, OpenTelemetry openTelemetry) {
        this(metricPrefix, openTelemetry.getMeter("marketdata.stats.telemetry"));
    }

    TelemetryStatsSink(String metricPrefix, Meter meter) {
        this.metricPrefix = Objects.requireNonNull(metricPrefix, "metricPrefix must not be null");
        this.meter = Objects.requireNonNull(meter, "meter must not be null");
    }

    @Override
    public void publish(StatsSnapshot snapshot) {
        Attributes attrs = Attributes.of(SNAPSHOT_KEY, snapshot.name());

        snapshot.counters().forEach((name, value) -> {
            if (value > 0) {
                String metricName = sanitize(metricPrefix + "_" + name + "_total");
                counters.computeIfAbsent(metricName, key ->
                        meter.counterBuilder(metricName)
                                .setDescription("Stats counter")
                                .build()
                ).add(value, attrs);
            }
        });

        snapshot.gauges().forEach((name, value) -> {
            String metricName = sanitize(metricPrefix + "_" + name);
            registerGaugeIfNeeded(metricName);
            gaugeValues
                    .computeIfAbsent(metricName, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(attrs, ignored -> new AtomicReference<>(0.0))
                    .set((double) value);
        });

        snapshot.latencies().forEach((name, latency) -> {
            String avgMetricName = sanitize(metricPrefix + "_" + name + "_avg_ms");
            registerGaugeIfNeeded(avgMetricName);
            gaugeValues
                    .computeIfAbsent(avgMetricName, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(attrs, ignored -> new AtomicReference<>(0.0))
                    .set(latency.avg());

            String maxMetricName = sanitize(metricPrefix + "_" + name + "_max_ms");
            registerGaugeIfNeeded(maxMetricName);
            gaugeValues
                    .computeIfAbsent(maxMetricName, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(attrs, ignored -> new AtomicReference<>(0.0))
                    .set((double) latency.max());
        });
    }

    private void registerGaugeIfNeeded(String metricName) {
        registeredGauges.computeIfAbsent(metricName, key ->
                meter.gaugeBuilder(metricName)
                        .setDescription("Stats gauge")
                        .buildWithCallback(measurement -> {
                            Map<Attributes, AtomicReference<Double>> values = gaugeValues.get(metricName);
                            if (values != null) {
                                values.forEach((attrs, holder) -> {
                                    Double value = holder.get();
                                    if (value != null) {
                                        measurement.record(value.doubleValue(), attrs);
                                    }
                                });
                            }
                        })
        );
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
}