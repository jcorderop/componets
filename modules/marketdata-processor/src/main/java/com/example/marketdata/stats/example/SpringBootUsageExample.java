package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
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
 * Simply inject ServiceStatsCollector wherever you need to collect metrics.
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

        log.info("Stats collection example complete. StatsReporter will publish every minute.");
    }

    private void simulateMarketDataProcessing() {
        // Consume stage - track events from different consumer services
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(100);
        stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(50);
        stats.counter(MetricName.CONSUMED_EVENTS).add(150);

        // Pipeline stage - track processing
        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(150);
        stats.latency(MetricName.PIPELINE_LATENCY_MAX_MS).record(250);
        stats.latency(MetricName.PIPELINE_LATENCY_AVG_MS).record(180);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(150);

        // Dispatched stage - track dispatchers
        stats.counter(MetricName.DISPATCHED_EVENTS).add(150);
        stats.latency(MetricName.DISPATCHED_ZMQ_MAX_MS).record(80);
        stats.latency(MetricName.DISPATCHED_ZMQ_AVG_MS).record(65);

        stats.latency(MetricName.DISPATCHED_HAZELCAST_MAX_MS).record(120);
        stats.latency(MetricName.DISPATCHED_HAZELCAST_AVG_MS).record(95);
        stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(2);

        // Storage stage - track database persistence
        stats.counter(MetricName.STORAGE_EVENTS).add(150);
        stats.latency(MetricName.STORAGE_POSTGRES_MAX_MS).record(350);
        stats.latency(MetricName.STORAGE_POSTGRES_AVG_MS).record(280);
        stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);

        log.info("Simulated market data processing with {} events", 150);
    }
}
