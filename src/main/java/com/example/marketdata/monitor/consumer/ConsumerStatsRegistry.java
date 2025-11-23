package com.example.marketdata.monitor.consumer;

import java.util.List;

public interface ConsumerStatsRegistry {
    void recordEnqueue(String consumer);
    void recordDrop(String consumer);
    void recordBatchProcessed(String consumer,
                              int batchSize,
                              long durationMillis);
    void recordQueueSize(String consumer,
                         int queueSize);

    List<ConsumerStatsSnapshot> snapshotAndReset();
}

