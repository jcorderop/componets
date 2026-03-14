package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.sink.LoggerStatsSinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Example Spring Boot component demonstrating statistics collection.
 *
 * The ServiceStatsCollector is automatically injected by Spring.
 * The StatsReporter automatically runs every minute via @Scheduled annotation.
 *
 * This example touches all MetricName constants once so you can see how they appear in logs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringBootUsageExample implements CommandLineRunner {

    private final ServiceStatsCollector stats;

    @Override
    public void run(String... args) {
        log.info("Starting Spring Boot stats collection example");

        simulateMarketDataProcessing();

        // Optional immediate preview of what LoggerStatsSinkService logs.
        new LoggerStatsSinkService().publish(stats.snapshotAndReset());

        log.info("Stats collection example complete. StatsReporter will publish every minute.");
    }

    private void simulateMarketDataProcessing() {
        // Consume stage
        stats.counter(MetricName.CONSUMED_EVENTS).add(4);
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_RFA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_BPIPE_EVENTS).add(1);

        // Pipeline stage
        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(4);
        stats.latency(MetricName.PIPELINE_LATENCY).record(170);
        stats.latency(MetricName.PIPELINE_LATENCY).record(210);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(4);

        // Dispatched stage - ZMQ
        stats.counter(MetricName.DISPATCHED_EVENTS).add(2);
        stats.latency(MetricName.DISPATCHED_ZMQ_LATENCY_MS).record(80);
        stats.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(10);

        // Dispatched stage - Hazelcast
        stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS).add(1);
        stats.latency(MetricName.DISPATCHED_HAZELCAST_LATENCY_MS).record(95);
        stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE).setMax(5);

        // Dispatched stage - Kafka
        stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS).add(1);
        stats.latency(MetricName.DISPATCHED_KAFKA_LATENCY_MS).record(135);
        stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED).add(1);
        stats.gauge(MetricName.DISPATCHED_KAFKA_QUEUE_SIZE).setMax(2);

        // Storage stage
        stats.counter(MetricName.STORAGE_POSTGRES_EVENTS).add(2);
        stats.latency(MetricName.STORAGE_POSTGRES_LATENCY_MS).record(280);
        stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);

        stats.counter(MetricName.STORAGE_ORACLE_EVENTS).add(2);
        stats.latency(MetricName.STORAGE_ORACLE_LATENCY_MS).record(320);
        stats.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED).add(1);

        log.info("Simulated market data processing covering all MetricName constants");
    }
}
