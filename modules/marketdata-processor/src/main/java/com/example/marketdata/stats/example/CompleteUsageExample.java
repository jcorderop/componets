package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import com.example.marketdata.stats.sink.IStatsSink;
import com.example.marketdata.stats.sink.LoggerStatsSinkService;
import com.example.marketdata.stats.sink.PrometheusStatsSinkService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Complete example demonstrating the statistics collection and reporting system.
 * Shows how to:
 * 1. Collect statistics during processing
 * 2. Configure sinks for different outputs
 * 3. Manual snapshot and reporting (standalone mode)
 */
@Slf4j
public class CompleteUsageExample {


    public static void main(String[] args) {
        log.info("Starting standalone stats collection example");

        // 1. Create the stats collector
        ServiceStatsCollector stats = new ServiceStatsCollector();

        // 2. Configure sinks (can use multiple)
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        List<IStatsSink> sinks = List.of(
                new LoggerStatsSinkService(),
                new PrometheusStatsSinkService("marketdata", meterRegistry)
        );

        // 3. Simulate market data processing
        simulateMarketDataProcessing(stats);

        // 4. Manual snapshot and publish
        log.info("Taking manual snapshot and publishing to sinks");
        StatsSnapshot snapshot = stats.snapshotAndReset();
        for (IStatsSink sink : sinks) {
            sink.publish(snapshot);
        }

        log.info("Example complete. In production, repeat snapshot every minute.");
    }

    private static void simulateMarketDataProcessing(final ServiceStatsCollector stats) {
        log.info("Simulating market data processing...");

        // Consume stage - Kafka consumer
        for (int i = 0; i < 1000; i++) {
            stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);
            stats.counter(MetricName.CONSUMED_EVENTS).add(1);
        }

        // Consume stage - FIX consumer
        for (int i = 0; i < 500; i++) {
            stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(1);
            stats.counter(MetricName.CONSUMED_EVENTS).add(1);
        }

        // Consume stage - RFA consumer
        for (int i = 0; i < 300; i++) {
            stats.counter(MetricName.CONSUMED_RFA_EVENTS).add(1);
            stats.counter(MetricName.CONSUMED_EVENTS).add(1);
        }


        // Consume stage - BPIPE consumer
        for (int i = 0; i < 200; i++) {
            stats.counter(MetricName.CONSUMED_BPIPE_EVENTS).add(1);
            stats.counter(MetricName.CONSUMED_EVENTS).add(1);
        }

        // Pipeline processing with latency tracking
        int totalConsumed = 2000;
        for (int i = 0; i < totalConsumed; i++) {
            stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(1);
            stats.latency(MetricName.PIPELINE_LATENCY).record(100 + (i % 50));
            stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(1);
        }

        // Dispatched stage - ZeroMQ dispatcher
        for (int i = 0; i < 800; i++) {
            stats.counter(MetricName.DISPATCHED_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_ZMQ_LATENCY_MS).record(50 + (i % 30));
            // Simulate some dropped events
            if (i % 100 == 0) {
                stats.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED).add(1);
            }
        }

        // Dispatched stage - Hazelcast dispatcher
        for (int i = 0; i < 600; i++) {
            stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_HAZELCAST_LATENCY_MS).record(80 + (i % 40));
            // Simulate some dropped events
            if (i % 150 == 0) {
                stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(1);
            }
        }

        // Dispatched stage - Kafka dispatcher
        for (int i = 0; i < 400; i++) {
            stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_KAFKA_LATENCY_MS).record(200 + (i % 100));
            if (i % 160 == 0) {
                stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED).add(1);
            }
        }

        // Storage stage - Postgres
        for (int i = 0; i < 900; i++) {
            stats.counter(MetricName.STORAGE_POSTGRES_EVENTS).add(1);
            stats.latency(MetricName.STORAGE_POSTGRES_LATENCY_MS).record(300 + (i % 50));
            // Simulate some dropped events
            if (i % 200 == 0) {
                stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);
            }
        }

        // Storage stage - Oracle
        for (int i = 0; i < 900; i++) {
            stats.counter(MetricName.STORAGE_ORACLE_EVENTS).add(1);
            stats.latency(MetricName.STORAGE_ORACLE_LATENCY_MS).record(350 + (i % 60));
            // Simulate some dropped events
            if (i % 180 == 0) {
                stats.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED).add(1);
            }
        }

        // Gauge usage (e.g., current in-memory queue depth)
        stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(27);
        stats.gauge(MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE).setMax(12);
        stats.gauge(MetricName.DISPATCHED_KAFKA_QUEUE_SIZE).setMax(8);

        log.info("Processing complete.");
    }
}
