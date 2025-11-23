package com.example.marketdata.monitor.consumer;

import lombok.Builder;

@Builder
public record ConsumerStatsSnapshot(
        String consumerName,
        long windowStartMillis,
        long windowEndMillis,
        long eventsEnqueued,
        long eventsProcessed,
        long eventsDropped,
        long minLatencyMillis,
        long maxLatencyMillis,
        double avgLatencyMillis,
        int queueSizeAtSnapshot
) {}

