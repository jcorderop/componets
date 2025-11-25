package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.processor.ProcessorStatsSink;
import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.util.AtomicDouble;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ProcessorStatsSink} that exports processor metrics to Micrometer so they can be scraped
 * via the Prometheus actuator endpoint. Counters are incremented for throughput metrics and gauges
 * expose the latest latency, queue depth and reporting window boundaries per processor.
 */
@Service
public class ProcessorStatsSinkPrometheusService implements ProcessorStatsSink {

    private static final String TAG_PROCESSOR = "processor";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> queueSizeGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> windowStartGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> windowEndGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> minLatencyGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> maxLatencyGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicDouble> avgLatencyGauges = new ConcurrentHashMap<>();

    public ProcessorStatsSinkPrometheusService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(List<ProcessorStatsSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (ProcessorStatsSnapshot snapshot : snapshots) {
            Tags tags = Tags.of(TAG_PROCESSOR, snapshot.processorName());

            meterRegistry.counter("marketdata.processor.events.enqueued", tags)
                    .increment(snapshot.eventsEnqueued());
            meterRegistry.counter("marketdata.processor.events.processed", tags)
                    .increment(snapshot.eventsProcessed());
            meterRegistry.counter("marketdata.processor.events.dropped", tags)
                    .increment(snapshot.eventsDropped());

            updateLongGauge(
                    "marketdata.processor.latency.min", snapshot.processorName(), tags,
                    minLatencyGauges, snapshot.minLatencyMillis(), "milliseconds");
            updateLongGauge(
                    "marketdata.processor.latency.max", snapshot.processorName(), tags,
                    maxLatencyGauges, snapshot.maxLatencyMillis(), "milliseconds");
            updateDoubleGauge(
                    "marketdata.processor.latency.avg", snapshot.processorName(), tags,
                    avgLatencyGauges, snapshot.avgLatencyMillis(), "milliseconds");

            updateLongGauge(
                    "marketdata.processor.queue.size", snapshot.processorName(), tags,
                    queueSizeGauges, snapshot.queueSizeAtSnapshot(), "events");
            updateLongGauge(
                    "marketdata.processor.window.start", snapshot.processorName(), tags,
                    windowStartGauges, snapshot.windowStartMillis(), "milliseconds");
            updateLongGauge(
                    "marketdata.processor.window.end", snapshot.processorName(), tags,
                    windowEndGauges, snapshot.windowEndMillis(), "milliseconds");
        }
    }

    private void updateLongGauge(String metricName,
                                 String processorName,
                                 Tags tags,
                                 ConcurrentMap<String, AtomicLong> gauges,
                                 long value,
                                 String baseUnit) {
        gauges.computeIfAbsent(processorName, key -> {
            AtomicLong holder = new AtomicLong(value);
            Gauge.Builder<AtomicLong> builder = Gauge.builder(metricName, holder, AtomicLong::get)
                    .tags(tags);
            if (baseUnit != null) {
                builder.baseUnit(baseUnit);
            }
            builder.register(meterRegistry);
            return holder;
        }).set(value);
    }

    private void updateDoubleGauge(String metricName,
                                   String processorName,
                                   Tags tags,
                                   ConcurrentMap<String, AtomicDouble> gauges,
                                   double value,
                                   String baseUnit) {
        gauges.computeIfAbsent(processorName, key -> {
            AtomicDouble holder = new AtomicDouble(value);
            Gauge.Builder<AtomicDouble> builder = Gauge.builder(metricName, holder, AtomicDouble::get)
                    .tags(tags);
            if (baseUnit != null) {
                builder.baseUnit(baseUnit);
            }
            builder.register(meterRegistry);
            return holder;
        }).set(value);
    }
}
