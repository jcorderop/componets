package com.example.marketdata.monitor.processor;

import java.util.List;

/**
 * Collects runtime metrics for each processor and provides snapshots for reporting.
 * Implementations are responsible for tracking counts and timing information between
 * reporting windows and returning immutable snapshots when requested. Latency metrics are
 * tracked in milliseconds.
 */
public interface ProcessorStatsRegistry {
    void recordEnqueue(String processor);
    default void recordDrop(String processor) {
        recordDrops(processor, 1);
    }
    void recordDrops(String processor, int dropCount);
    void recordBatchProcessed(String processor,
                              int batchSize,
                              long durationMillis);
    void recordQueueSize(String processor,
                         int queueSize);

    List<ProcessorStatsSnapshot> snapshotAndReset();
}

