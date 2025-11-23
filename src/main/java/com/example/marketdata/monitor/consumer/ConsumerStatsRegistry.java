package com.example.marketdata.monitor.consumer;

import java.util.List;

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

