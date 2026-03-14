package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryStatsSinkTest {

    private static final AttributeKey<String> SNAPSHOT = AttributeKey.stringKey("snapshot");

    private final InMemoryMetricReader reader = InMemoryMetricReader.create();
    private final SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .registerMetricReader(reader)
            .build();
    private final OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(meterProvider)
            .build();

    @AfterEach
    void tearDown() {
        meterProvider.close();
    }

    @Test
    void publishExportsCountersAndGaugesForApmPipeline() {
        TelemetryStatsSink sink = new TelemetryStatsSink("marketdata_telemetry", openTelemetry);

        sink.publish(new StatsSnapshot(
                "snapshot-a",
                Map.of("events.received", 10L),
                Map.of("queue-size", 5L),
                Map.of("pipeline.latency", new StatsSnapshot.LatencySnapshot(11.3, 19.8))
        ));

        List<MetricData> metrics = reader.collectAllMetrics();
        assertEquals(10L, longSumValue(metrics, "marketdata_telemetry_events_received_total", "snapshot-a"));
        assertEquals(5L, longGaugeValue(metrics, "marketdata_telemetry_queue_size", "snapshot-a"));
        assertEquals(11.3, doubleGaugeValue(metrics, "marketdata_telemetry_pipeline_latency_avg_ms", "snapshot-a"));
        assertEquals(19.8, doubleGaugeValue(metrics, "marketdata_telemetry_pipeline_latency_max_ms", "snapshot-a"));
    }

    @Test
    void publishAccumulatesCounterAndUpdatesGaugeValues() {
        TelemetryStatsSink sink = new TelemetryStatsSink("md", openTelemetry);

        sink.publish(new StatsSnapshot(
                "snapshot-a",
                Map.of("events", 1L),
                Map.of("queue", 2L),
                Map.of("lat", new StatsSnapshot.LatencySnapshot(1.0, 3.0))
        ));

        sink.publish(new StatsSnapshot(
                "snapshot-a",
                Map.of("events", 4L),
                Map.of("queue", 9L),
                Map.of("lat", new StatsSnapshot.LatencySnapshot(2.5, 7.5))
        ));

        List<MetricData> metrics = reader.collectAllMetrics();
        assertEquals(5L, longSumValue(metrics, "md_events_total", "snapshot-a"));
        assertEquals(9L, longGaugeValue(metrics, "md_queue", "snapshot-a"));
        assertEquals(2.5, doubleGaugeValue(metrics, "md_lat_avg_ms", "snapshot-a"));
        assertEquals(7.5, doubleGaugeValue(metrics, "md_lat_max_ms", "snapshot-a"));
    }

    private long longSumValue(List<MetricData> metrics, String metricName, String snapshotName) {
        Attributes attrs = Attributes.of(SNAPSHOT, snapshotName);
        return metrics.stream()
                .filter(metric -> metric.getName().equals(metricName))
                .flatMap(metric -> metric.getLongSumData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attrs))
                .mapToLong(LongPointData::getValue)
                .findFirst()
                .orElse(0L);
    }

    private long longGaugeValue(List<MetricData> metrics, String metricName, String snapshotName) {
        Attributes attrs = Attributes.of(SNAPSHOT, snapshotName);
        return metrics.stream()
                .filter(metric -> metric.getName().equals(metricName))
                .flatMap(metric -> metric.getLongGaugeData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attrs))
                .mapToLong(LongPointData::getValue)
                .findFirst()
                .orElse(0L);
    }

    private double doubleGaugeValue(List<MetricData> metrics, String metricName, String snapshotName) {
        Attributes attrs = Attributes.of(SNAPSHOT, snapshotName);
        return metrics.stream()
                .filter(metric -> metric.getName().equals(metricName))
                .flatMap(metric -> metric.getDoubleGaugeData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attrs))
                .mapToDouble(DoublePointData::getValue)
                .findFirst()
                .orElse(0.0);
    }
}
