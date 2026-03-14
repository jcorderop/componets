package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import com.example.marketdata.stats.sink.LoggerStatsSink;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple example showing all available MetricName constants and how they are logged.
 */
@Slf4j
public class SimpleUsageExample {

    public static void main(String[] args) {
        ServiceStatsCollector stats = new ServiceStatsCollector();

        // Consume stage metrics
        stats.counter(MetricName.CONSUMED_EVENTS).add(4);
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_RFA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_BPIPE_EVENTS).add(1);

        // Pipeline stage metrics
        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(4);
        stats.latency(MetricName.PIPELINE_LATENCY).record(180);
        stats.latency(MetricName.PIPELINE_LATENCY).record(220);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(4);

        // Dispatched stage metrics
        stats.counter(MetricName.DISPATCHED_EVENTS).add(2);
        stats.latency(MetricName.DISPATCHED_ZMQ_LATENCY_MS).record(80);
        stats.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(15);

        stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS).add(1);
        stats.latency(MetricName.DISPATCHED_HAZELCAST_LATENCY_MS).record(95);
        stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE).setMax(7);

        stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS).add(1);
        stats.latency(MetricName.DISPATCHED_KAFKA_LATENCY_MS).record(120);
        stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_KAFKA_QUEUE_SIZE).setMax(3);

        // Storage stage metrics
        stats.counter(MetricName.STORAGE_POSTGRES_EVENTS).add(2);
        stats.latency(MetricName.STORAGE_POSTGRES_LATENCY_MS).record(280);
        stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);

        stats.counter(MetricName.STORAGE_ORACLE_EVENTS).add(2);
        stats.latency(MetricName.STORAGE_ORACLE_LATENCY_MS).record(320);
        stats.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED).add(1);

        // Show exactly how metrics are logged by LoggerStatsSink
        StatsSnapshot snapshot = stats.snapshotAndReset();
        new LoggerStatsSink().publish(snapshot);

        log.info("Simple example complete.");
    }
}
