package com.example.marketdata.monitor.consumer;

import lombok.Builder;

/**
 * Immutable view of the metrics accumulated for a consumer during a single reporting window.
 * The snapshot captures throughput counts, latency statistics (all in milliseconds) and the
 * queue depth at the time the window closed so sinks can publish a consistent picture of
 * consumer health.
 * Latency values represent processing duration per event in milliseconds, computed from
 * batch processing times and batch sizes. They do not include queueing delays.
 */
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

