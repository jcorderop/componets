package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.snapshot.StatsSnapshot;

/**
 * Interface for publishing statistics snapshots to external systems.
 * Implementations can output to logs, Prometheus, Elasticsearch, etc.
 */
public interface IStatsSink {

    /**
     * Publish a statistics snapshot.
     * @param snapshot the snapshot to publish
     */
    void publish(StatsSnapshot snapshot);
}
