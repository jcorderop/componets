package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.metric.ICounterMetric;
import com.example.marketdata.stats.metric.ILatencyMetric;

/**
 * Base API for storage-stage metrics.
 */
public abstract class AbstractStorageStats implements IWrapperStats {

    private final ICounterMetric stored;
    private final ILatencyMetric latency;
    private final ICounterMetric dropped;

    protected AbstractStorageStats(
            final ICounterMetric stored,
            final ILatencyMetric latency,
            final ICounterMetric dropped
    ) {
        this.stored = stored;
        this.latency = latency;
        this.dropped = dropped;
    }

    public void addStored(final long delta) {
        stored.add(delta);
    }

    public void recordLatency(final long latencyMs) {
        latency.record(latencyMs);
    }

    public void addDropped(final long delta) {
        dropped.add(delta);
    }
}
