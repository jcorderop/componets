package com.example.marketdata.monitor.consumer;

import java.util.List;

/**
 * Collects runtime metrics for each consumer and provides snapshots for reporting.
 * Implementations are responsible for tracking counts and timing information between
 * reporting windows and returning immutable snapshots when requested.
 */
public interface ConsumerStatsRegistry {
    void recordEnqueue(String consumer);
    default void recordDrop(String consumer) {
        recordDrops(consumer, 1);
    }
    void recordDrops(String consumer, int dropCount);
    void recordBatchProcessed(String consumer,
                              int batchSize,
                              long durationMillis);
    void recordQueueSize(String consumer,
                         int queueSize);

    List<ConsumerStatsSnapshot> snapshotAndReset();
}

