package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.metric.ICounterMetric;
import com.example.marketdata.stats.metric.ILatencyMetric;

/**
 * Base API for pipeline-stage metrics.
 */
public abstract class AbstractPipelineStats {

    private final ICounterMetric received;
    private final ILatencyMetric latency;
    private final ICounterMetric forwarded;

    protected AbstractPipelineStats(
            final ICounterMetric received,
            final ILatencyMetric latency,
            final ICounterMetric forwarded
    ) {
        this.received = received;
        this.latency = latency;
        this.forwarded = forwarded;
    }

    public void addReceived(final long delta) {
        received.add(delta);
    }

    public void recordLatency(final long latencyMs) {
        latency.record(latencyMs);
    }

    public void addForwarded(final long delta) {
        forwarded.add(delta);
    }
}
