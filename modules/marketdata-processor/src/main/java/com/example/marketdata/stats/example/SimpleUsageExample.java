package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple example showing basic statistics collection using constants.
 */
@Slf4j
public class SimpleUsageExample {

    public static void main(String[] args) {

        ServiceStatsCollector stats = new ServiceStatsCollector();

        // Consume stage - specific consumer service
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(1);

        // Consume stage - total consumed events
        stats.counter(MetricName.CONSUMED_EVENTS).add(2);

        // Pipeline stage
        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(2);
        stats.latency(MetricName.PIPELINE_LATENCY_MAX_MS).record(250);
        stats.latency(MetricName.PIPELINE_LATENCY_AVG_MS).record(180);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(2);

        // Dispatched stage
        stats.counter(MetricName.DISPATCHED_EVENTS).add(2);
        stats.latency(MetricName.DISPATCHED_ZMQ_MAX_MS).record(80);
        stats.latency(MetricName.DISPATCHED_ZMQ_AVG_MS).record(65);

        stats.latency(MetricName.DISPATCHED_HAZELCAST_MAX_MS).record(120);
        stats.latency(MetricName.DISPATCHED_HAZELCAST_AVG_MS).record(95);

        log.info("Stats recorded.");
    }
}
