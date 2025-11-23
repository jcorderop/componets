package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.consumer.ConsumerStatsSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConsumerStatsSinkLoggerServiceTest {

    @Test
    void publishDoesNotThrowAndLogsSnapshots() {
        ConsumerStatsSinkLoggerService sink = new ConsumerStatsSinkLoggerService();

        ConsumerStatsSnapshot snapshot = ConsumerStatsSnapshot.builder()
                .consumerName("logger-consumer")
                .windowStartMillis(1L)
                .windowEndMillis(2L)
                .eventsEnqueued(1L)
                .eventsProcessed(1L)
                .eventsDropped(0L)
                .minLatencyMillis(1L)
                .maxLatencyMillis(1L)
                .avgLatencyMillis(1.0)
                .queueSizeAtSnapshot(0)
                .build();

        sink.publish(List.of(snapshot));
    }
}
