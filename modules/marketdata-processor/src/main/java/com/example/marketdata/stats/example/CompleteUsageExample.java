package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.sink.IStatsSink;
import com.example.marketdata.stats.sink.LoggerStatsSink;
import com.example.marketdata.stats.sink.PrometheusStatsSink;
import com.example.marketdata.stats.snapshot.StatsSnapshot;
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
        List<IStatsSink> sinks = List.of(
                new LoggerStatsSink(),
                new PrometheusStatsSink("marketdata")
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

    private static void simulateMarketDataProcessing(ServiceStatsCollector stats) {
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

        // Pipeline processing with latency tracking
        int totalConsumed = 1800;
        for (int i = 0; i < totalConsumed; i++) {
            stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(1);
            stats.latency(MetricName.PIPELINE_LATENCY_MAX_MS).record(100 + (i % 50));
            stats.latency(MetricName.PIPELINE_LATENCY_AVG_MS).record(80 + (i % 40));
            stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(1);
        }

        // Dispatched stage - ZeroMQ dispatcher
        for (int i = 0; i < 800; i++) {
            stats.counter(MetricName.DISPATCHED_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_ZMQ_MAX_MS).record(50 + (i % 30));
            stats.latency(MetricName.DISPATCHED_ZMQ_AVG_MS).record(40 + (i % 20));
            // Simulate some dropped events
            if (i % 100 == 0) {
                stats.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED).add(1);
            }
        }

        // Dispatched stage - Hazelcast dispatcher
        for (int i = 0; i < 600; i++) {
            stats.counter(MetricName.DISPATCHED_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_HAZELCAST_MAX_MS).record(80 + (i % 40));
            stats.latency(MetricName.DISPATCHED_HAZELCAST_AVG_MS).record(60 + (i % 30));
            // Simulate some dropped events
            if (i % 150 == 0) {
                stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(1);
            }
        }

        // Dispatched stage - Kafka dispatcher
        for (int i = 0; i < 400; i++) {
            stats.counter(MetricName.DISPATCHED_EVENTS).add(1);
            stats.latency(MetricName.DISPATCHED_KAFKA_MAX_MS).record(200 + (i % 100));
            stats.latency(MetricName.DISPATCHED_KAFKA_AVG_MS).record(150 + (i % 80));
        }

        // Storage stage - Postgres
        for (int i = 0; i < 900; i++) {
            stats.counter(MetricName.STORAGE_EVENTS).add(1);
            stats.latency(MetricName.STORAGE_POSTGRES_MAX_MS).record(300 + (i % 50));
            stats.latency(MetricName.STORAGE_POSTGRES_AVG_MS).record(250 + (i % 40));
            // Simulate some dropped events
            if (i % 200 == 0) {
                stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);
            }
        }

        // Storage stage - Oracle
        for (int i = 0; i < 900; i++) {
            stats.counter(MetricName.STORAGE_EVENTS).add(1);
            stats.latency(MetricName.STORAGE_ORACLE_MAX_MS).record(350 + (i % 60));
            stats.latency(MetricName.STORAGE_ORACLE_AVG_MS).record(280 + (i % 50));
            // Simulate some dropped events
            if (i % 180 == 0) {
                stats.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED).add(1);
            }
        }

        // Gauge usage (e.g., current in-memory queue depth)
        stats.gauge(MetricName.FORWARD_ZMQ_QUEUE_SIZE).set(27);

        // Optional direct reads from latency metric API
        double latestPipelineAvg = stats.latency(MetricName.PIPELINE_LATENCY_AVG_MS).avg();
        long latestPipelineMax = stats.latency(MetricName.PIPELINE_LATENCY_MAX_MS).max();
        log.info("Current ZMQ queue depth={}, pipeline avg={}, pipeline max={}",
                stats.gauge(MetricName.FORWARD_ZMQ_QUEUE_SIZE).value(), latestPipelineAvg, latestPipelineMax);

        log.info("Processing complete.");
    }
}
